package cn.ai.sales.module.agent.service.webhook;

import cn.ai.sales.framework.tenant.core.util.TenantUtils;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentGeweCredentialDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWebhookEventDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentGeweCredentialMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWebhookEventMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.gewe.GeweContactInfo;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.ai.sales.module.agent.service.gewe.GeweScalarNormalizer;
import cn.ai.sales.module.agent.service.conversation.AgentWechatDisplayFormatter;
import cn.ai.sales.module.agent.service.reply.AgentAutoReplyService;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WEBHOOK_ACCOUNT_NOT_FOUND;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WEBHOOK_PAYLOAD_INVALID;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WEBHOOK_SIGNATURE_INVALID;

@Service
@Validated
@Slf4j
public class AgentWebhookServiceImpl implements AgentWebhookService {

    private static final String SIGNATURE_PREFIX = "sha256=";

    @Resource
    private AgentGeweCredentialMapper geweCredentialMapper;
    @Resource
    private AgentWechatAccountMapper wechatAccountMapper;
    @Resource
    private AgentWebhookEventMapper webhookEventMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private AgentConversationMapper conversationMapper;
    @Resource
    private AgentMessageMapper messageMapper;
    @Resource
    private GeweCallbackParser geweCallbackParser;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private AgentAutoReplyService autoReplyService;
    @Resource
    private GeweMessageClient geweMessageClient;

    @Override
    public void handleGeweCallback(String callbackToken, Map<String, Object> payload, String signature) {
        AgentGeweCredentialDO credential = TenantUtils.executeIgnore(
                () -> geweCredentialMapper.selectByCallbackToken(callbackToken));
        if (credential != null) {
            handleGeweCredentialCallback(credential, payload, signature);
            return;
        }

        AgentWechatAccountDO account = TenantUtils.executeIgnore(
                () -> wechatAccountMapper.selectByCallbackToken(callbackToken));
        if (account == null || !Objects.equals(account.getStatus(), AgentConstants.STATUS_ENABLE)) {
            throw exception(WEBHOOK_ACCOUNT_NOT_FOUND);
        }
        if (!validateSignature(account.getCallbackSecret(), payload, signature)) {
            throw exception(WEBHOOK_SIGNATURE_INVALID);
        }

        TenantUtils.execute(account.getTenantId(), () -> handleGeweCallback(account, payload));
    }

    private void handleGeweCredentialCallback(AgentGeweCredentialDO credential, Map<String, Object> payload,
                                              String signature) {
        if (!Objects.equals(credential.getStatus(), AgentConstants.STATUS_ENABLE)) {
            throw exception(WEBHOOK_ACCOUNT_NOT_FOUND);
        }
        if (!validateSignature(credential.getCallbackSecret(), payload, signature)) {
            throw exception(WEBHOOK_SIGNATURE_INVALID);
        }

        GeweCallbackMessage callbackMessage = geweCallbackParser.parse(payload);
        TenantUtils.execute(credential.getTenantId(), () -> {
            AgentWechatAccountDO account = resolveAccount(credential, callbackMessage);
            if (account == null || !Objects.equals(account.getStatus(), AgentConstants.STATUS_ENABLE)) {
                throw exception(WEBHOOK_ACCOUNT_NOT_FOUND);
            }
            handleGeweCallback(account, callbackMessage, payload);
        });
    }

    private void handleGeweCallback(AgentWechatAccountDO account, Map<String, Object> payload) {
        GeweCallbackMessage callbackMessage = geweCallbackParser.parse(payload);
        handleGeweCallback(account, callbackMessage, payload);
    }

    private void handleGeweCallback(AgentWechatAccountDO account, GeweCallbackMessage callbackMessage,
                                    Map<String, Object> payload) {
        if (StrUtil.isNotBlank(account.getWechatId())
                && !StrUtil.equals(callbackMessage.ownerWxid(), account.getWechatId())) {
            throw exception(WEBHOOK_PAYLOAD_INVALID);
        }
        if (StrUtil.isNotBlank(account.getGeweAppId())
                && !StrUtil.equals(callbackMessage.geweAppId(), account.getGeweAppId())) {
            refreshCurrentAppId(account, callbackMessage.geweAppId());
        }

        AgentWebhookEventDO existedEvent = webhookEventMapper.selectByAccountAndEventId(
                account.getId(), callbackMessage.eventId());
        if (existedEvent != null) {
            return;
        }

        AgentWebhookEventDO event = buildWebhookEvent(account, callbackMessage, payload);
        webhookEventMapper.insert(event);
        try {
            AgentMessageDO message = saveMessage(account, callbackMessage, payload);
            markEventProcessed(event.getId());
            log.info("[handleGeweCallback][accountId({}) eventId({}) messageId({}) processed]",
                    account.getId(), callbackMessage.eventId(), message.getId());
        } catch (RuntimeException ex) {
            markEventFailed(event.getId(), ex);
            throw ex;
        }
    }

    private AgentWebhookEventDO buildWebhookEvent(AgentWechatAccountDO account, GeweCallbackMessage callbackMessage,
                                                 Map<String, Object> payload) {
        AgentWebhookEventDO event = new AgentWebhookEventDO();
        event.setWechatAccountId(account.getId());
        event.setEventId(callbackMessage.eventId());
        event.setEventType(callbackMessage.eventType());
        event.setSignatureValid(true);
        event.setRawPayload(payload);
        event.setProcessStatus(AgentConstants.WEBHOOK_STATUS_NEW);
        return event;
    }

    private AgentMessageDO saveMessage(AgentWechatAccountDO account, GeweCallbackMessage callbackMessage,
                                       Map<String, Object> payload) {
        AgentWechatContactDO contact = getOrCreateContact(account, callbackMessage);
        AgentConversationDO conversation = getOrCreateConversation(account, contact);
        AgentMessageDO existedMessage = messageMapper.selectByGeweMessageId(
                account.getId(), contact.getId(), callbackMessage.geweMessageId());
        if (existedMessage != null) {
            return existedMessage;
        }

        AgentMessageDO message = new AgentMessageDO();
        message.setConversationId(conversation.getId());
        message.setWechatAccountId(account.getId());
        message.setContactId(contact.getId());
        message.setDirection(callbackMessage.selfSent()
                ? AgentConstants.MESSAGE_DIRECTION_OUTBOUND : AgentConstants.MESSAGE_DIRECTION_INBOUND);
        message.setSenderType(callbackMessage.selfSent()
                ? AgentConstants.SENDER_HUMAN_ADVISOR : AgentConstants.SENDER_CUSTOMER);
        message.setMessageType(callbackMessage.messageType());
        message.setContent(resolveMessageContent(account, callbackMessage));
        message.setRawPayload(payload);
        message.setGeweMessageId(callbackMessage.geweMessageId());
        message.setSendStatus(AgentConstants.SEND_STATUS_RECEIVED);
        message.setMessageTime(callbackMessage.messageTime());
        messageMapper.insert(message);

        updateContactAfterMessage(contact.getId(), callbackMessage.messageTime());
        updateConversationAfterMessage(conversation.getId(), message.getId(), callbackMessage.messageTime());
        if (!callbackMessage.selfSent()) {
            autoReplyService.handleInboundMessage(account, contact, conversation, message);
        }
        return message;
    }

    private AgentWechatAccountDO resolveAccount(AgentGeweCredentialDO credential, GeweCallbackMessage callbackMessage) {
        AgentWechatAccountDO account = null;
        if (StrUtil.isNotBlank(callbackMessage.ownerWxid())) {
            account = wechatAccountMapper.selectByCredentialAndWxid(credential.getId(), callbackMessage.ownerWxid());
        }
        if (account == null && StrUtil.isNotBlank(callbackMessage.geweAppId())) {
            account = wechatAccountMapper.selectByCredentialAndAppId(credential.getId(), callbackMessage.geweAppId());
        }
        if (account == null && StrUtil.isNotBlank(callbackMessage.ownerWxid())) {
            account = wechatAccountMapper.selectByWxid(callbackMessage.ownerWxid());
        }
        if (account == null && StrUtil.isNotBlank(callbackMessage.geweAppId())) {
            account = wechatAccountMapper.selectByGeweAppId(callbackMessage.geweAppId());
        }
        return account;
    }

    private void refreshCurrentAppId(AgentWechatAccountDO account, String appId) {
        if (StrUtil.isBlank(appId)) {
            return;
        }
        AgentWechatAccountDO update = new AgentWechatAccountDO();
        update.setId(account.getId());
        update.setGeweAppId(appId);
        update.setGeweAccountId(appId);
        update.setLastHeartbeatTime(LocalDateTime.now());
        wechatAccountMapper.updateById(update);
        account.setGeweAppId(appId);
        account.setGeweAccountId(appId);
    }

    private AgentWechatContactDO getOrCreateContact(AgentWechatAccountDO account, GeweCallbackMessage callbackMessage) {
        AgentWechatContactDO contact = contactMapper.selectByAccountAndExternalUserId(
                account.getId(), callbackMessage.contactWxid());
        if (contact != null) {
            refreshContactName(account, contact, callbackMessage);
            return contact;
        }

        contact = new AgentWechatContactDO();
        contact.setWechatAccountId(account.getId());
        contact.setExternalUserId(callbackMessage.contactWxid());
        contact.setWechatId(callbackMessage.contactWxid());
        applyContactDisplayInfo(contact, account, callbackMessage);
        contact.setOwnerUserId(account.getOwnerUserId());
        contact.setCustomerLevel(AgentConstants.CUSTOMER_LEVEL_NORMAL);
        contact.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        contact.setLastMessageTime(callbackMessage.messageTime());
        contact.setLastConversationStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        contactMapper.insert(contact);
        return contact;
    }

    private void refreshContactName(AgentWechatAccountDO account, AgentWechatContactDO contact,
                                    GeweCallbackMessage callbackMessage) {
        GeweContactInfo contactInfo = null;
        String resolvedName = resolveContactName(callbackMessage);
        if (!isUsableContactName(resolvedName, contact.getExternalUserId())) {
            contactInfo = geweMessageClient.getContactInfo(account, contact.getExternalUserId());
            resolvedName = resolveContactName(callbackMessage, contactInfo);
        }
        if (!isUsableContactName(resolvedName, contact.getExternalUserId())
                || StrUtil.equals(resolvedName, contact.getNickname())) {
            return;
        }
        AgentWechatContactDO update = new AgentWechatContactDO();
        update.setId(contact.getId());
        update.setNickname(resolvedName);
        if (contactInfo != null) {
            if (isUsableContactName(contactInfo.remark(), contact.getExternalUserId())) {
                update.setRemark(contactInfo.remark());
                contact.setRemark(contactInfo.remark());
            }
            if (StrUtil.isNotBlank(contactInfo.avatar())) {
                update.setAvatar(contactInfo.avatar());
                contact.setAvatar(contactInfo.avatar());
            }
        }
        contactMapper.updateById(update);
        contact.setNickname(resolvedName);
    }

    private String resolveContactName(GeweCallbackMessage callbackMessage) {
        return firstUsableContactName(callbackMessage.contactWxid(),
                callbackMessage.groupDisplayName(),
                callbackMessage.contactDisplayName());
    }

    private String resolveMessageContent(AgentWechatAccountDO account, GeweCallbackMessage callbackMessage) {
        if (callbackMessage.selfSent() || !StrUtil.endWith(callbackMessage.contactWxid(), "@chatroom")) {
            return callbackMessage.content();
        }
        String memberWxid = StrUtil.blankToDefault(callbackMessage.groupMemberWxid(),
                AgentWechatDisplayFormatter.extractGroupMemberWxidPrefix(callbackMessage.content()));
        if (StrUtil.isBlank(memberWxid)) {
            return callbackMessage.content();
        }
        String memberName = firstUsableContactName(memberWxid, callbackMessage.groupMemberDisplayName());
        if (StrUtil.isBlank(memberName)) {
            GeweContactInfo memberInfo = geweMessageClient.getChatroomMemberInfo(account, callbackMessage.contactWxid(),
                    memberWxid);
            memberName = firstUsableContactName(memberWxid,
                    memberInfo == null ? null : memberInfo.remark(),
                    memberInfo == null ? null : memberInfo.nickname());
        }
        return AgentWechatDisplayFormatter.replaceGroupMemberWxidPrefix(callbackMessage.content(), memberName);
    }

    private String resolveContactName(GeweCallbackMessage callbackMessage, GeweContactInfo contactInfo) {
        return firstUsableContactName(callbackMessage.contactWxid(),
                contactInfo == null ? null : contactInfo.remark(),
                contactInfo == null ? null : contactInfo.nickname(),
                callbackMessage.groupDisplayName(),
                callbackMessage.contactDisplayName());
    }

    private void applyContactDisplayInfo(AgentWechatContactDO contact, AgentWechatAccountDO account,
                                         GeweCallbackMessage callbackMessage) {
        GeweContactInfo contactInfo = geweMessageClient.getContactInfo(account, callbackMessage.contactWxid());
        contact.setNickname(resolveContactName(callbackMessage, contactInfo));
        if (contactInfo != null) {
            contact.setRemark(contactInfo.remark());
            contact.setAvatar(contactInfo.avatar());
        }
    }

    private boolean isUsableContactName(String name, String wxid) {
        String text = GeweScalarNormalizer.cleanText(name);
        return StrUtil.isNotBlank(text)
                && !StrUtil.equals(text, wxid)
                && !GeweScalarNormalizer.isRawWechatIdentifier(text);
    }

    private String firstUsableContactName(String wxid, String... names) {
        for (String name : names) {
            if (isUsableContactName(name, wxid)) {
                return GeweScalarNormalizer.cleanText(name);
            }
        }
        return null;
    }

    private AgentConversationDO getOrCreateConversation(AgentWechatAccountDO account, AgentWechatContactDO contact) {
        AgentConversationDO conversation = conversationMapper.selectByAccountAndContact(account.getId(), contact.getId());
        if (conversation != null) {
            return conversation;
        }

        conversation = new AgentConversationDO();
        conversation.setAgentId(account.getAgentId());
        conversation.setWechatAccountId(account.getId());
        conversation.setContactId(contact.getId());
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        conversation.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        conversation.setContinuousAutoReplyCount(0);
        conversationMapper.insert(conversation);
        return conversation;
    }

    private void updateContactAfterMessage(Long contactId, LocalDateTime messageTime) {
        AgentWechatContactDO updateObj = new AgentWechatContactDO();
        updateObj.setId(contactId);
        updateObj.setLastMessageTime(messageTime);
        updateObj.setLastConversationStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        contactMapper.updateById(updateObj);
    }

    private void updateConversationAfterMessage(Long conversationId, Long messageId, LocalDateTime messageTime) {
        AgentConversationDO updateObj = new AgentConversationDO();
        updateObj.setId(conversationId);
        updateObj.setLastMessageId(messageId);
        updateObj.setLastMessageTime(messageTime);
        conversationMapper.updateById(updateObj);
    }

    private void markEventProcessed(Long eventId) {
        AgentWebhookEventDO updateObj = new AgentWebhookEventDO();
        updateObj.setId(eventId);
        updateObj.setProcessStatus(AgentConstants.WEBHOOK_STATUS_PROCESSED);
        webhookEventMapper.updateById(updateObj);
    }

    private void markEventFailed(Long eventId, RuntimeException ex) {
        AgentWebhookEventDO updateObj = new AgentWebhookEventDO();
        updateObj.setId(eventId);
        updateObj.setProcessStatus(AgentConstants.WEBHOOK_STATUS_FAILED);
        updateObj.setErrorMessage(StrUtil.maxLength(ex.getMessage(), 512));
        webhookEventMapper.updateById(updateObj);
    }

    private boolean validateSignature(String secret, Map<String, Object> payload, String signature) {
        if (StrUtil.isBlank(secret)) {
            return true;
        }
        if (StrUtil.isBlank(signature)) {
            return false;
        }
        String normalizedSignature = StrUtil.removePrefixIgnoreCase(signature.trim(), SIGNATURE_PREFIX);
        String expectedSignature = hmacSha256Hex(secret, toCanonicalJson(payload));
        return MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                normalizedSignature.getBytes(StandardCharsets.UTF_8));
    }

    private String toCanonicalJson(Map<String, Object> payload) {
        try {
            ObjectMapper copy = objectMapper.copy();
            copy.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            return copy.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw exception(WEBHOOK_PAYLOAD_INVALID);
        }
    }

    private String hmacSha256Hex(String secret, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("计算 Gewe 回调签名失败", ex);
        }
    }

}
