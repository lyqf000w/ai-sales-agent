package cn.ai.sales.module.agent.service.conversation;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.framework.security.core.util.SecurityFrameworkUtils;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactPageReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateTagsReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationContactRespVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationContactSettingsRespVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationContactSettingsSaveReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationMessageReviewReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationPageReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationRestoreOriginalPolicyReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationSendMessageReqVO;
import cn.ai.sales.module.agent.controller.admin.replypolicy.vo.AgentReplyPolicyRespVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentReplyDecisionDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentReplyDecisionMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.contact.AgentWechatContactDisplayService;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.ai.sales.module.agent.service.gewe.GeweMessageTimeNormalizer;
import cn.ai.sales.module.agent.service.gewe.GeweTextSendResult;
import cn.ai.sales.module.agent.service.reply.AgentReplyPolicy;
import cn.ai.sales.module.agent.service.reply.AgentReplyPolicyActivationService;
import cn.ai.sales.module.agent.service.reply.AgentReplyPolicyResolver;
import cn.ai.sales.module.agent.service.review.AgentReplyReviewService;
import cn.ai.sales.module.agent.service.tag.AgentContactTagService;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.CONVERSATION_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.CONVERSATION_STATUS_INVALID;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.GEWE_SEND_CONFIG_MISSING;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.GEWE_SEND_FAILED;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.MESSAGE_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.MESSAGE_STATUS_INVALID;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WECHAT_CONTACT_NOT_EXISTS;

@Service
@Validated
public class AgentConversationServiceImpl implements AgentConversationService {

    @Resource
    private AgentConversationMapper conversationMapper;
    @Resource
    private AgentMessageMapper messageMapper;
    @Resource
    private AgentReplyDecisionMapper decisionMapper;
    @Resource
    private AgentWechatAccountMapper accountMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private AgentWechatContactDisplayService contactDisplayService;
    @Resource
    private GeweMessageClient geweMessageClient;
    @Resource
    private AgentReplyReviewService replyReviewService;
    @Resource
    private AgentReplyPolicyResolver replyPolicyResolver;
    @Resource
    private AgentReplyPolicyActivationService replyPolicyActivationService;
    @Resource
    private AgentContactTagService tagService;

    @Override
    public PageResult<AgentConversationDO> getConversationPage(AgentConversationPageReqVO pageReqVO) {
        return conversationMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<AgentConversationContactRespVO> getConversationContactPage(AgentWechatContactPageReqVO pageReqVO) {
        PageResult<AgentWechatContactDO> pageResult = contactMapper.selectConversationQueuePage(pageReqVO);
        List<AgentConversationContactRespVO> list = pageResult.getList().stream()
                .map(this::buildConversationContactRespVO)
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<AgentMessageDO> getConversationMessages(Long conversationId) {
        AgentConversationDO conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw exception(CONVERSATION_NOT_EXISTS);
        }
        List<AgentMessageDO> messages = messageMapper.selectListByConversationId(conversationId);
        AgentWechatContactDO contact = contactMapper.selectById(conversation.getContactId());
        List<AgentMessageDO> visibleMessages = filterPrivateConversationGroupArtifacts(contact, messages);
        visibleMessages.forEach(message -> {
            repairMessageTimeIfNeeded(message);
        });
        return visibleMessages;
    }

    @Override
    public Long sendMessage(AgentConversationSendMessageReqVO sendReqVO) {
        AgentConversationDO conversation = validateConversationExists(sendReqVO.getConversationId());
        AgentWechatAccountDO account = validateCanSend(conversation.getWechatAccountId());
        AgentWechatContactDO contact = contactMapper.selectById(conversation.getContactId());

        GeweTextSendResult sendResult = geweMessageClient.sendText(account, contact.getExternalUserId(),
                sendReqVO.getContent());
        AgentMessageDO message = buildOutboundMessage(conversation, account, contact, AgentConstants.SENDER_HUMAN_ADVISOR,
                sendReqVO.getContent());
        message.setGeweMessageId(sendResult.geweMessageId());
        message.setRawPayload(sendResult.rawResponse());
        message.setSendStatus(sendResult.success() ? AgentConstants.SEND_STATUS_SENT : AgentConstants.SEND_STATUS_FAILED);
        message.setAuditNote(sendResult.success() ? "人工发送成功" : StrUtil.maxLength(sendResult.errorMessage(), 512));
        messageMapper.insert(message);
        if (!sendResult.success()) {
            updateConversationLastMessage(conversation.getId(), message.getId(), message.getMessageTime());
            throw exception(GEWE_SEND_FAILED);
        }
        invalidatePendingAiSuggestions(conversation.getId());
        updateConversationAfterManualSent(conversation, message.getId(), message.getMessageTime());
        return message.getId();
    }

    @Override
    public void restoreOriginalPolicy(AgentConversationRestoreOriginalPolicyReqVO restoreReqVO) {
        AgentConversationDO conversation = validateConversationExists(restoreReqVO.getConversationId());
        if (!Objects.equals(conversation.getStatus(), AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER)) {
            throw exception(CONVERSATION_STATUS_INVALID);
        }

        AgentConversationDO conversationUpdate = new AgentConversationDO();
        conversationUpdate.setId(conversation.getId());
        conversationUpdate.setStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        conversationUpdate.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        conversationUpdate.setContinuousAutoReplyCount(0);
        conversationMapper.updateById(conversationUpdate);
        conversationMapper.clearTemporaryTakeover(conversation.getId());

        AgentWechatContactDO contactUpdate = new AgentWechatContactDO();
        contactUpdate.setId(conversation.getContactId());
        contactUpdate.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        contactUpdate.setLastConversationStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        contactMapper.updateById(contactUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acknowledgePendingView(AgentConversationRestoreOriginalPolicyReqVO reqVO) {
        AgentConversationDO conversation = validateConversationExists(reqVO.getConversationId());
        if (!Objects.equals(conversation.getStatus(), AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM)) {
            return false;
        }
        List<AgentReplyDecisionDO> pendingDecisions = decisionMapper.selectPendingListByConversationId(conversation.getId());
        if (hasActionablePendingSuggestion(pendingDecisions)) {
            return false;
        }
        rejectNonActionablePendingDecisions(pendingDecisions);

        AgentConversationDO conversationUpdate = new AgentConversationDO();
        conversationUpdate.setId(conversation.getId());
        conversationUpdate.setStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        conversationUpdate.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        conversationUpdate.setContinuousAutoReplyCount(0);
        conversationMapper.updateById(conversationUpdate);
        conversationMapper.clearPendingReply(conversation.getId());

        AgentWechatContactDO contactUpdate = new AgentWechatContactDO();
        contactUpdate.setId(conversation.getContactId());
        contactUpdate.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        contactUpdate.setLastConversationStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        contactMapper.updateById(contactUpdate);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentConversationContactSettingsRespVO saveContactSettings(AgentConversationContactSettingsSaveReqVO saveReqVO) {
        AgentConversationDO conversation = validateConversationExists(saveReqVO.getConversationId());
        if (!Objects.equals(conversation.getContactId(), saveReqVO.getContactId())) {
            throw exception(WECHAT_CONTACT_NOT_EXISTS);
        }
        AgentWechatContactDO contact = contactMapper.selectById(saveReqVO.getContactId());
        if (contact == null) {
            throw exception(WECHAT_CONTACT_NOT_EXISTS);
        }

        AgentWechatContactDO contactUpdate = new AgentWechatContactDO();
        contactUpdate.setId(contact.getId());
        contactUpdate.setCustomerLevel(defaultIfNull(saveReqVO.getCustomerLevel(), AgentConstants.CUSTOMER_LEVEL_NORMAL));
        contactUpdate.setPurchaseIntention(defaultIfBlank(saveReqVO.getPurchaseIntention(),
                AgentConstants.PURCHASE_INTENTION_MEDIUM));
        contactUpdate.setSalesStage(defaultIfBlank(saveReqVO.getSalesStage(), AgentConstants.SALES_STAGE_NEW_LEAD));
        contactUpdate.setCustomerSentiment(defaultIfBlank(saveReqVO.getCustomerSentiment(),
                AgentConstants.CUSTOMER_SENTIMENT_NEUTRAL));
        contactUpdate.setFollowUpPriority(defaultIfBlank(saveReqVO.getFollowUpPriority(),
                AgentConstants.FOLLOW_UP_PRIORITY_NORMAL));
        contactMapper.updateById(contactUpdate);

        AgentWechatContactDO policyUpdate = new AgentWechatContactDO();
        policyUpdate.setId(contact.getId());
        policyUpdate.setReplyMode(normalizeContactReplyMode(saveReqVO.getReplyMode()));
        policyUpdate.setQuietSeconds(normalizeContactQuietSeconds(saveReqVO.getQuietSeconds()));
        policyUpdate.setBusinessHours(normalizeContactBusinessHours(saveReqVO.getBusinessHours()));
        contactMapper.updateReplyPolicyById(policyUpdate);

        AgentWechatContactUpdateTagsReqVO tagReqVO = new AgentWechatContactUpdateTagsReqVO();
        tagReqVO.setContactId(contact.getId());
        tagReqVO.setTagIds(saveReqVO.getTagIds() == null ? Collections.emptyList() : saveReqVO.getTagIds());
        tagService.updateContactTags(tagReqVO);

        AgentWechatContactDO latestContact = contactMapper.selectById(contact.getId());
        AgentWechatAccountDO account = accountMapper.selectById(latestContact.getWechatAccountId());
        AgentConversationContactSettingsRespVO respVO = new AgentConversationContactSettingsRespVO();
        respVO.setContact(buildConversationContactRespVO(latestContact));
        respVO.setPolicy(buildReplyPolicyResp(account, latestContact, conversation.getId()));
        respVO.setTagIds(tagService.getContactTagIds(contact.getId()));
        if (AgentConstants.REPLY_MODE_AUTO_REPLY.equals(respVO.getPolicy().getReplyMode())) {
            replyPolicyActivationService.activateContactAutoReply(account.getId(), latestContact.getId(),
                    conversation.getId());
            latestContact = contactMapper.selectById(contact.getId());
            respVO.setContact(buildConversationContactRespVO(latestContact));
            respVO.setPolicy(buildReplyPolicyResp(account, latestContact, conversation.getId()));
        }
        return respVO;
    }

    @Override
    public void approveMessage(AgentConversationMessageReviewReqVO reviewReqVO) {
        replyReviewService.approveByMessage(reviewReqVO);
    }

    @Override
    public void rejectMessage(AgentConversationMessageReviewReqVO reviewReqVO) {
        replyReviewService.rejectByMessage(reviewReqVO);
    }

    private AgentConversationDO validateConversationExists(Long id) {
        AgentConversationDO conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            throw exception(CONVERSATION_NOT_EXISTS);
        }
        return conversation;
    }

    private AgentMessageDO validatePendingReviewMessage(Long id) {
        AgentMessageDO message = messageMapper.selectById(id);
        if (message == null) {
            throw exception(MESSAGE_NOT_EXISTS);
        }
        if (!Objects.equals(message.getSendStatus(), AgentConstants.SEND_STATUS_PENDING_REVIEW)) {
            throw exception(MESSAGE_STATUS_INVALID);
        }
        return message;
    }

    private AgentConversationContactRespVO buildConversationContactRespVO(AgentWechatContactDO contact) {
        AgentConversationContactRespVO respVO = BeanUtils.toBean(contact, AgentConversationContactRespVO.class);
        respVO.setId(contact.getId());
        respVO.setContactId(contact.getId());
        respVO.setExternalUserId(AgentWechatDisplayFormatter.cleanScalar(respVO.getExternalUserId()));
        respVO.setWechatId(AgentWechatDisplayFormatter.cleanScalar(respVO.getWechatId()));
        respVO.setNickname(AgentWechatDisplayFormatter.cleanScalar(respVO.getNickname()));
        respVO.setRemark(AgentWechatDisplayFormatter.cleanScalar(respVO.getRemark()));
        respVO.setDisplayName(contactDisplayService.resolveDisplayName(contact));
        respVO.setLastMessageTime(normalizeTime(respVO.getLastMessageTime()));
        AgentConversationDO conversation = conversationMapper.selectByAccountAndContact(contact.getWechatAccountId(),
                contact.getId());
        if (conversation == null) {
            conversation = createEmptyConversation(contact);
        }
        respVO.setConversationId(conversation.getId());
        respVO.setAgentId(conversation.getAgentId());
        respVO.setStatus(conversation.getStatus());
        respVO.setRiskLevel(conversation.getRiskLevel());
        respVO.setLastMessageId(conversation.getLastMessageId());
        respVO.setLastMessageTime(normalizeTime(conversation.getLastMessageTime()));
        respVO.setContinuousAutoReplyCount(conversation.getContinuousAutoReplyCount());
        respVO.setHumanTakeoverUserId(conversation.getHumanTakeoverUserId());
        respVO.setHumanTakeoverTime(conversation.getHumanTakeoverTime());
        repairConversationTimeIfNeeded(contact, conversation);
        return respVO;
    }

    private AgentConversationDO createEmptyConversation(AgentWechatContactDO contact) {
        AgentWechatAccountDO account = accountMapper.selectById(contact.getWechatAccountId());
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setAgentId(account == null ? null : account.getAgentId());
        conversation.setWechatAccountId(contact.getWechatAccountId());
        conversation.setContactId(contact.getId());
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        conversation.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        conversation.setContinuousAutoReplyCount(0);
        conversationMapper.insert(conversation);
        return conversation;
    }

    private String resolveDisplayName(AgentWechatContactDO contact) {
        String name = contactDisplayService.resolveDisplayName(contact);
        return StrUtil.blankToDefault(name, "客户 #" + contact.getId());
    }

    private LocalDateTime normalizeTime(LocalDateTime time) {
        return GeweMessageTimeNormalizer.normalize(time);
    }

    private void repairConversationTimeIfNeeded(AgentWechatContactDO contact, AgentConversationDO conversation) {
        LocalDateTime normalizedContactTime = normalizeTime(contact.getLastMessageTime());
        if (!Objects.equals(contact.getLastMessageTime(), normalizedContactTime)) {
            AgentWechatContactDO contactUpdate = new AgentWechatContactDO();
            contactUpdate.setId(contact.getId());
            contactUpdate.setLastMessageTime(normalizedContactTime);
            contactMapper.updateById(contactUpdate);
            contact.setLastMessageTime(normalizedContactTime);
        }

        LocalDateTime normalizedConversationTime = normalizeTime(conversation.getLastMessageTime());
        if (!Objects.equals(conversation.getLastMessageTime(), normalizedConversationTime)) {
            AgentConversationDO conversationUpdate = new AgentConversationDO();
            conversationUpdate.setId(conversation.getId());
            conversationUpdate.setLastMessageTime(normalizedConversationTime);
            conversationMapper.updateById(conversationUpdate);
            conversation.setLastMessageTime(normalizedConversationTime);
        }
    }

    private void repairMessageTimeIfNeeded(AgentMessageDO message) {
        LocalDateTime normalized = normalizeTime(message.getMessageTime());
        if (Objects.equals(message.getMessageTime(), normalized)) {
            return;
        }
        AgentMessageDO update = new AgentMessageDO();
        update.setId(message.getId());
        update.setMessageTime(normalized);
        messageMapper.updateById(update);
        message.setMessageTime(normalized);
    }

    private List<AgentMessageDO> filterPrivateConversationGroupArtifacts(AgentWechatContactDO contact,
                                                                         List<AgentMessageDO> messages) {
        if (contact == null || StrUtil.endWith(AgentWechatDisplayFormatter.cleanScalar(contact.getExternalUserId()),
                "@chatroom")) {
            return messages;
        }
        String privateExternalUserId = AgentWechatDisplayFormatter.cleanScalar(contact.getExternalUserId());
        return messages.stream()
                .filter(message -> !isLegacyGroupMessageInPrivateConversation(message, privateExternalUserId))
                .toList();
    }

    private boolean isLegacyGroupMessageInPrivateConversation(AgentMessageDO message, String privateExternalUserId) {
        if (message == null) {
            return false;
        }
        if (hasExplicitGroupOriginMarker(message.getRawPayload())) {
            return !hasTopLevelPrivateConversationMarker(message.getRawPayload(), privateExternalUserId)
                    || hasTopLevelGroupOriginMarker(message.getRawPayload());
        }
        return false;
    }

    private boolean hasTopLevelPrivateConversationMarker(Object value, String privateExternalUserId) {
        if (StrUtil.isBlank(privateExternalUserId) || !(value instanceof Map<?, ?> map)) {
            return false;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String text = AgentWechatDisplayFormatter.cleanScalar(String.valueOf(entry.getValue()));
            if (isConversationSideKey(key) && StrUtil.equals(text, privateExternalUserId)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTopLevelGroupOriginMarker(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return false;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String text = AgentWechatDisplayFormatter.cleanScalar(String.valueOf(entry.getValue()));
            if (isGroupOriginKey(key) && StrUtil.endWith(text, "@chatroom")) {
                return true;
            }
            if (isConversationSideKey(key) && StrUtil.endWith(text, "@chatroom")) {
                return true;
            }
            if (StrUtil.equalsAnyIgnoreCase(key, "eventCode", "event_code", "typeName", "type_name")
                    && StrUtil.containsIgnoreCase(text, "group_msg_event")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExplicitGroupOriginMarker(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String text = AgentWechatDisplayFormatter.cleanScalar(String.valueOf(entry.getValue()));
                if (isGroupOriginKey(key) && StrUtil.endWith(text, "@chatroom")) {
                    return true;
                }
                if (isConversationSideKey(key) && StrUtil.endWith(text, "@chatroom")) {
                    return true;
                }
                if (StrUtil.equalsAnyIgnoreCase(key, "eventCode", "event_code", "typeName", "type_name")
                        && StrUtil.containsIgnoreCase(text, "group_msg_event")) {
                    return true;
                }
                if (hasExplicitGroupOriginMarker(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (hasExplicitGroupOriginMarker(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isGroupOriginKey(String key) {
        return StrUtil.equalsAnyIgnoreCase(key,
                "chatroomId", "chatRoomId", "chatroomWxid", "chatRoomWxid", "roomWxid",
                "groupId", "groupWxid", "fromGroup", "from_group", "fromGroupWxid", "fromChatroom");
    }

    private boolean isConversationSideKey(String key) {
        return StrUtil.equalsAnyIgnoreCase(key,
                "fromUser", "fromWxid", "FromUserName", "from_user", "toUser", "toWxid",
                "ToUserName", "to_user");
    }

    private AgentReplyPolicyRespVO buildReplyPolicyResp(AgentWechatAccountDO account, AgentWechatContactDO contact,
                                                        Long conversationId) {
        AgentReplyPolicy policy = replyPolicyResolver.resolve(account, contact);
        AgentReplyPolicyRespVO respVO = new AgentReplyPolicyRespVO();
        respVO.setWechatAccountId(contact.getWechatAccountId());
        respVO.setContactId(contact.getId());
        respVO.setConversationId(conversationId);
        respVO.setReplyMode(policy.replyMode());
        respVO.setQuietSeconds(policy.quietSeconds());
        respVO.setBusinessHours(policy.businessHours());
        respVO.setSource(policy.source());
        return respVO;
    }

    private String normalizeContactReplyMode(String replyMode) {
        if (StrUtil.isBlank(replyMode)) {
            return null;
        }
        if (AgentConstants.REPLY_MODE_MANUAL_ONLY.equals(replyMode)) {
            return AgentConstants.REPLY_MODE_MANUAL_CONFIRM;
        }
        return replyMode;
    }

    private Integer normalizeContactQuietSeconds(Integer quietSeconds) {
        return quietSeconds == null ? null : Math.max(quietSeconds, 1);
    }

    private Map<String, Object> normalizeContactBusinessHours(Map<String, Object> businessHours) {
        if (businessHours == null || businessHours.isEmpty()) {
            return null;
        }
        Object start = businessHours.get("start");
        Object end = businessHours.get("end");
        if (start == null || end == null || StrUtil.isBlank(String.valueOf(start)) || StrUtil.isBlank(String.valueOf(end))) {
            return null;
        }
        return Map.of("start", String.valueOf(start), "end", String.valueOf(end));
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StrUtil.blankToDefault(value, defaultValue);
    }

    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private AgentWechatAccountDO validateCanSend(Long accountId) {
        AgentWechatAccountDO account = accountMapper.selectById(accountId);
        if (!geweMessageClient.canSend(account)) {
            throw exception(GEWE_SEND_CONFIG_MISSING);
        }
        return account;
    }

    private AgentMessageDO buildOutboundMessage(AgentConversationDO conversation, AgentWechatAccountDO account,
                                                AgentWechatContactDO contact, Integer senderType, String content) {
        AgentMessageDO message = new AgentMessageDO();
        message.setConversationId(conversation.getId());
        message.setWechatAccountId(account.getId());
        message.setContactId(contact.getId());
        message.setDirection(AgentConstants.MESSAGE_DIRECTION_OUTBOUND);
        message.setSenderType(senderType);
        message.setMessageType(AgentConstants.MESSAGE_TYPE_TEXT);
        message.setContent(content);
        message.setMessageTime(LocalDateTime.now());
        return message;
    }

    private void invalidatePendingAiSuggestions(Long conversationId) {
        List<AgentReplyDecisionDO> pendingDecisions = decisionMapper.selectPendingListByConversationId(conversationId);
        for (AgentReplyDecisionDO decision : pendingDecisions) {
            if (decision.getSuggestedMessageId() != null) {
                AgentMessageDO messageUpdate = new AgentMessageDO();
                messageUpdate.setId(decision.getSuggestedMessageId());
                messageUpdate.setSendStatus(AgentConstants.SEND_STATUS_REJECTED);
                messageUpdate.setAuditNote("人工已回复，AI 建议作废");
                messageUpdate.setOperatorUserId(getLoginUserIdQuietly());
                messageMapper.updateById(messageUpdate);
            }

            AgentReplyDecisionDO decisionUpdate = new AgentReplyDecisionDO();
            decisionUpdate.setId(decision.getId());
            decisionUpdate.setReviewStatus(AgentConstants.REVIEW_STATUS_REJECTED);
            decisionUpdate.setReviewNote("人工已回复，AI 建议作废");
            decisionUpdate.setReviewUserId(getLoginUserIdQuietly());
            decisionUpdate.setReviewTime(LocalDateTime.now());
            decisionMapper.updateById(decisionUpdate);
        }
    }

    private boolean hasActionablePendingSuggestion(List<AgentReplyDecisionDO> pendingDecisions) {
        for (AgentReplyDecisionDO decision : pendingDecisions) {
            if (decision.getSuggestedMessageId() == null) {
                continue;
            }
            AgentMessageDO suggestedMessage = messageMapper.selectById(decision.getSuggestedMessageId());
            if (suggestedMessage != null
                    && Objects.equals(suggestedMessage.getSendStatus(), AgentConstants.SEND_STATUS_PENDING_REVIEW)) {
                return true;
            }
        }
        return false;
    }

    private void rejectNonActionablePendingDecisions(List<AgentReplyDecisionDO> pendingDecisions) {
        for (AgentReplyDecisionDO decision : pendingDecisions) {
            AgentReplyDecisionDO decisionUpdate = new AgentReplyDecisionDO();
            decisionUpdate.setId(decision.getId());
            decisionUpdate.setReviewStatus(AgentConstants.REVIEW_STATUS_REJECTED);
            decisionUpdate.setReviewNote("人工已查看，会话待确认提醒关闭");
            decisionUpdate.setReviewUserId(getLoginUserIdQuietly());
            decisionUpdate.setReviewTime(LocalDateTime.now());
            decisionMapper.updateById(decisionUpdate);
        }
    }

    private void updateConversationAfterManualSent(AgentConversationDO conversation, Long messageId,
                                                   LocalDateTime messageTime) {
        Integer nextStatus = Objects.equals(conversation.getStatus(), AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER)
                ? AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER : AgentConstants.CONVERSATION_STATUS_OPEN;

        AgentConversationDO conversationUpdate = new AgentConversationDO();
        conversationUpdate.setId(conversation.getId());
        conversationUpdate.setStatus(nextStatus);
        conversationUpdate.setRiskLevel(Objects.equals(nextStatus, AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER)
                ? conversation.getRiskLevel() : AgentConstants.RISK_LEVEL_GREEN);
        conversationUpdate.setLastMessageId(messageId);
        conversationUpdate.setLastMessageTime(messageTime);
        conversationUpdate.setContinuousAutoReplyCount(0);
        conversationMapper.updateById(conversationUpdate);

        AgentWechatContactDO contactUpdate = new AgentWechatContactDO();
        contactUpdate.setId(conversation.getContactId());
        contactUpdate.setRiskLevel(conversationUpdate.getRiskLevel());
        contactUpdate.setLastConversationStatus(nextStatus);
        contactUpdate.setLastMessageTime(messageTime);
        contactMapper.updateById(contactUpdate);
    }

    private void updateConversationLastMessage(Long conversationId, Long messageId, LocalDateTime messageTime) {
        AgentConversationDO updateObj = new AgentConversationDO();
        updateObj.setId(conversationId);
        updateObj.setLastMessageId(messageId);
        updateObj.setLastMessageTime(messageTime);
        conversationMapper.updateById(updateObj);
    }

    private Long getLoginUserIdQuietly() {
        try {
            return SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

}
