package cn.ai.sales.module.agent.service.diagnostics;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentDiagnosticsGenerationRespVO;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentDiagnosticsSummaryRespVO;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentWebhookEventPageReqVO;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentWebhookEventRespVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentReplyDecisionDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWebhookEventDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentGeweCredentialMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentReplyDecisionMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWebhookEventMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.service.gewe.GeweContactInfo;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.ai.sales.module.agent.service.gewe.GeweScalarNormalizer;
import cn.ai.sales.module.agent.service.reply.AgentDeepSeekLlmProperties;
import cn.ai.sales.module.agent.service.reply.AgentGeneratedReply;
import cn.ai.sales.module.agent.service.webhook.GeweCallbackMessage;
import cn.ai.sales.module.agent.service.webhook.GeweCallbackParser;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception0;

@Service
@Validated
public class AgentDiagnosticsServiceImpl implements AgentDiagnosticsService {

    private static final int RECENT_DECISION_LIMIT = 20;

    @Resource
    private AgentDeepSeekLlmProperties deepSeekProperties;
    @Resource
    private AgentWechatAccountMapper accountMapper;
    @Resource
    private AgentGeweCredentialMapper credentialMapper;
    @Resource
    private AgentWebhookEventMapper webhookEventMapper;
    @Resource
    private AgentReplyDecisionMapper decisionMapper;
    @Resource
    private AgentConversationMapper conversationMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private GeweCallbackParser geweCallbackParser;
    @Resource
    private GeweMessageClient geweMessageClient;

    @Override
    public AgentDiagnosticsSummaryRespVO getSummary() {
        List<AgentReplyDecisionDO> recentDecisions = decisionMapper.selectRecentList(RECENT_DECISION_LIMIT);
        AgentWebhookEventDO latestWebhook = webhookEventMapper.selectLatest();
        AgentReplyDecisionDO latestDecision = decisionMapper.selectLatest();
        LocalDateTime today = LocalDate.now().atStartOfDay();

        AgentDiagnosticsSummaryRespVO respVO = new AgentDiagnosticsSummaryRespVO();
        respVO.setDeepSeekEnabled(Boolean.TRUE.equals(deepSeekProperties.getEnabled()));
        respVO.setDeepSeekApiKeyConfigured(StrUtil.isNotBlank(deepSeekProperties.getApiKey()));
        respVO.setDeepSeekUrl(deepSeekProperties.getUrl());
        respVO.setDeepSeekModel(deepSeekProperties.getModel());
        respVO.setWechatAccountCount(accountMapper.selectCount());
        respVO.setOnlineWechatAccountCount(accountMapper.selectOnlineCount());
        respVO.setGeweCredentialCount(credentialMapper.selectCount());
        respVO.setEnabledGeweCredentialCount(credentialMapper.selectEnabledCount());
        respVO.setTodayWebhookCount(webhookEventMapper.selectCountByCreateTimeBetween(today, today.plusDays(1)));
        respVO.setFailedWebhookCount(webhookEventMapper.selectFailedCount());
        respVO.setPendingReviewCount(decisionMapper.selectPendingReviewCount());
        respVO.setRiskConversationCount(conversationMapper.selectRiskCount());
        respVO.setRecentDeepSeekReplyCount(countGenerationSource(recentDecisions, AgentGeneratedReply.SOURCE_DEEPSEEK));
        respVO.setRecentKnowledgeReplyCount(countGenerationSource(recentDecisions, AgentGeneratedReply.SOURCE_KNOWLEDGE));
        respVO.setRecentFallbackReplyCount(countFallbackGeneration(recentDecisions));
        respVO.setLastWebhookTime(latestWebhook == null ? null : latestWebhook.getCreateTime());
        respVO.setLastReplyDecisionTime(latestDecision == null ? null : latestDecision.getCreateTime());
        respVO.setRecentGenerations(recentDecisions.stream().map(this::buildGenerationResp).toList());
        return respVO;
    }

    @Override
    public PageResult<AgentWebhookEventRespVO> getWebhookEventPage(AgentWebhookEventPageReqVO pageReqVO) {
        PageResult<AgentWebhookEventDO> pageResult = webhookEventMapper.selectPage(pageReqVO);
        List<AgentWebhookEventRespVO> list = pageResult.getList().stream()
                .map(this::buildWebhookResp)
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public AgentWebhookEventRespVO getWebhookEvent(Long id) {
        AgentWebhookEventDO event = webhookEventMapper.selectById(id);
        if (event == null) {
            throw exception0(1_010_000_999, "GeWe webhook event does not exist");
        }
        return buildWebhookResp(event);
    }

    private AgentWebhookEventRespVO buildWebhookResp(AgentWebhookEventDO event) {
        AgentWebhookEventRespVO respVO = BeanUtils.toBean(event, AgentWebhookEventRespVO.class);
        respVO.setEventTypeName(formatWebhookEventType(event.getEventType()));
        AgentWechatAccountDO account = event.getWechatAccountId() == null
                ? null : accountMapper.selectById(event.getWechatAccountId());
        if (account != null) {
            respVO.setWechatAccountName(StrUtil.blankToDefault(account.getNickname(), account.getWechatId()));
            respVO.setWechatId(account.getWechatId());
        }
        fillWebhookContactDisplay(respVO, event, account);
        return respVO;
    }

    private void fillWebhookContactDisplay(AgentWebhookEventRespVO respVO, AgentWebhookEventDO event,
                                           AgentWechatAccountDO account) {
        if (event.getRawPayload() == null || account == null) {
            respVO.setEventSummary(StrUtil.blankToDefault(event.getEventType(), event.getEventId()));
            return;
        }
        try {
            GeweCallbackMessage message = geweCallbackParser.parse(event.getRawPayload());
            respVO.setContactWxid(message.contactWxid());
            respVO.setEventTypeName(formatWebhookEventType(StrUtil.blankToDefault(message.eventType(), event.getEventType())));
            String groupDisplayName = resolveGroupDisplayName(account, message);
            String groupMemberDisplayName = resolveGroupMemberDisplayName(account, message);
            respVO.setGroupDisplayName(groupDisplayName);
            respVO.setGroupMemberDisplayName(groupMemberDisplayName);
            String displayName = StrUtil.isNotBlank(groupDisplayName) ? groupDisplayName
                    : resolveContactDisplayName(account, message);
            respVO.setContactDisplayName(displayName);
            String eventTarget = StrUtil.isNotBlank(groupDisplayName)
                    ? StrUtil.format("{} / {}", groupDisplayName,
                    StrUtil.blankToDefault(groupMemberDisplayName, "-"))
                    : StrUtil.blankToDefault(displayName, message.contactWxid());
            respVO.setEventSummary(StrUtil.format("{} / {}",
                    StrUtil.blankToDefault(respVO.getEventTypeName(),
                            StrUtil.blankToDefault(message.eventType(), event.getEventType())),
                    eventTarget));
        } catch (Exception ignored) {
            respVO.setEventSummary(StrUtil.blankToDefault(event.getEventType(), event.getEventId()));
        }
    }

    private String formatWebhookEventType(String eventType) {
        String text = GeweScalarNormalizer.cleanText(eventType);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        if (StrUtil.equalsAnyIgnoreCase(text, "group_msg_event", "group_msg", "group_message")) {
            return "群聊消息";
        }
        if (StrUtil.equalsAnyIgnoreCase(text, "private_msg_event", "friend_msg_event", "friend_msg", "private_message")) {
            return "私聊消息";
        }
        if (StrUtil.containsIgnoreCase(text, "group")) {
            return "群聊事件";
        }
        if (StrUtil.containsIgnoreCase(text, "private") || StrUtil.containsIgnoreCase(text, "friend")) {
            return "私聊事件";
        }
        return text;
    }

    private String resolveContactDisplayName(AgentWechatAccountDO account, GeweCallbackMessage message) {
        if (isChatroom(message.contactWxid())) {
            return resolveGroupDisplayName(account, message);
        }
        AgentWechatContactDO contact = contactMapper.selectByAccountAndExternalUserId(account.getId(),
                message.contactWxid());
        String fromDb = contact == null ? null : firstUsableName(message.contactWxid(),
                contact.getRemark(), contact.getNickname(), contact.getWechatId());
        if (StrUtil.isNotBlank(fromDb)) {
            return fromDb;
        }
        String fromPayload = firstUsableName(message.contactWxid(), message.groupDisplayName(),
                message.contactDisplayName());
        if (StrUtil.isNotBlank(fromPayload)) {
            return fromPayload;
        }
        GeweContactInfo info = geweMessageClient.getContactInfo(account, message.contactWxid());
        return firstUsableName(message.contactWxid(), info == null ? null : info.remark(),
                info == null ? null : info.nickname());
    }

    private String resolveGroupDisplayName(AgentWechatAccountDO account, GeweCallbackMessage message) {
        if (!isChatroom(message.contactWxid())) {
            return null;
        }
        AgentWechatContactDO contact = contactMapper.selectByAccountAndExternalUserId(account.getId(),
                message.contactWxid());
        String fromDb = contact == null ? null : firstUsableName(message.contactWxid(),
                contact.getRemark(), contact.getNickname(), contact.getWechatId());
        if (StrUtil.isNotBlank(fromDb)) {
            return fromDb;
        }
        String fromPayload = firstUsableName(message.contactWxid(), message.groupDisplayName());
        if (StrUtil.isNotBlank(fromPayload)) {
            return fromPayload;
        }
        GeweContactInfo info = geweMessageClient.getContactInfo(account, message.contactWxid());
        return firstUsableName(message.contactWxid(), info == null ? null : info.remark(),
                info == null ? null : info.nickname());
    }

    private String resolveGroupMemberDisplayName(AgentWechatAccountDO account, GeweCallbackMessage message) {
        if (!isChatroom(message.contactWxid())) {
            return null;
        }
        String memberWxid = message.groupMemberWxid();
        String fromPayload = firstUsableName(memberWxid, message.groupMemberDisplayName(), message.contactDisplayName());
        if (StrUtil.isNotBlank(fromPayload)) {
            return fromPayload;
        }
        if (StrUtil.isBlank(memberWxid)) {
            return null;
        }
        GeweContactInfo info = geweMessageClient.getChatroomMemberInfo(account, message.contactWxid(), memberWxid);
        return firstUsableName(memberWxid, info == null ? null : info.remark(),
                info == null ? null : info.nickname());
    }

    private boolean isChatroom(String wxid) {
        return StrUtil.endWith(wxid, "@chatroom");
    }

    private String firstUsableName(String wxid, String... names) {
        for (String name : names) {
            String text = GeweScalarNormalizer.cleanText(name);
            if (StrUtil.isNotBlank(text)
                    && !StrUtil.equals(text, wxid)
                    && !GeweScalarNormalizer.isRawWechatIdentifier(text)) {
                return text;
            }
        }
        return null;
    }

    private AgentDiagnosticsGenerationRespVO buildGenerationResp(AgentReplyDecisionDO decision) {
        AgentDiagnosticsGenerationRespVO respVO = new AgentDiagnosticsGenerationRespVO();
        respVO.setDecisionId(decision.getId());
        respVO.setConversationId(decision.getConversationId());
        respVO.setReviewStatus(decision.getReviewStatus());
        respVO.setCreateTime(decision.getCreateTime());
        Map<String, Object> refs = decision.getKnowledgeRefs();
        respVO.setGenerationSource(string(refs, "generationSource"));
        respVO.setLlmProvider(string(refs, "llmProvider"));
        respVO.setLlmModel(StrUtil.blankToDefault(string(refs, "llmModel"), decision.getLlmModel()));
        return respVO;
    }

    private Long countGenerationSource(List<AgentReplyDecisionDO> decisions, String source) {
        return decisions.stream()
                .filter(decision -> Objects.equals(string(decision.getKnowledgeRefs(), "generationSource"), source))
                .count();
    }

    private Long countFallbackGeneration(List<AgentReplyDecisionDO> decisions) {
        return decisions.stream()
                .filter(decision -> {
                    String source = string(decision.getKnowledgeRefs(), "generationSource");
                    return StrUtil.isBlank(source) || Objects.equals(source, AgentGeneratedReply.SOURCE_NONE);
                })
                .count();
    }

    private String string(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

}
