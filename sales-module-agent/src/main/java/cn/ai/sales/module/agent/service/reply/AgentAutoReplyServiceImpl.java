package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentReplyDecisionDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentSensitiveRuleDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentReplyDecisionMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentSensitiveRuleMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.ai.sales.module.agent.service.gewe.GeweTextSendResult;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Validated
@Slf4j
public class AgentAutoReplyServiceImpl implements AgentAutoReplyService {

    @Resource
    private AgentMapper agentMapper;
    @Resource
    private AgentSensitiveRuleMapper sensitiveRuleMapper;
    @Resource
    private AgentMessageMapper messageMapper;
    @Resource
    private AgentReplyDecisionMapper replyDecisionMapper;
    @Resource
    private AgentConversationMapper conversationMapper;
    @Resource
    private AgentWechatAccountMapper accountMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private AgentReplyGenerator replyGenerator;
    @Resource
    private AgentConversationContextBuilder contextBuilder;
    @Resource
    private AgentSensitiveRuleMatcher sensitiveRuleMatcher;
    @Resource
    private GeweMessageClient geweMessageClient;
    @Resource
    private AgentAutoReplyPolicyEvaluator autoReplyPolicyEvaluator;
    @Resource
    private AgentReplyPolicyResolver replyPolicyResolver;
    @Resource
    private AgentIntentRecognitionService intentRecognitionService;

    @Override
    public void handleInboundMessage(AgentWechatAccountDO account, AgentWechatContactDO contact,
                                     AgentConversationDO conversation, AgentMessageDO inboundMessage) {
        handleInboundMessage(account, contact, conversation, inboundMessage, true);
    }

    private void handleInboundMessage(AgentWechatAccountDO account, AgentWechatContactDO contact,
                                      AgentConversationDO conversation, AgentMessageDO inboundMessage,
                                      boolean respectQuietWindow) {
        if (!Objects.equals(inboundMessage.getDirection(), AgentConstants.MESSAGE_DIRECTION_INBOUND)
                || account.getAgentId() == null) {
            return;
        }
        boolean chatroomContact = isChatroomContact(contact);
        boolean mediaNeedsHumanReview = requiresHumanReviewForMedia(inboundMessage);
        if (chatroomContact && !mediaNeedsHumanReview) {
            updateRiskAndStatus(contact.getId(), conversation.getId(), AgentConstants.RISK_LEVEL_GREEN,
                    AgentConstants.CONVERSATION_STATUS_OPEN);
            return;
        }
        AgentDO agent = agentMapper.selectById(account.getAgentId());
        if (agent == null || !Objects.equals(agent.getStatus(), AgentConstants.STATUS_ENABLE)) {
            return;
        }

        AgentReplyPolicy replyPolicy = replyPolicyResolver.resolve(account, contact);
        if (mediaNeedsHumanReview) {
            recordMediaManualDecision(contact, conversation, inboundMessage, replyPolicy);
            return;
        }
        if (AgentConstants.REPLY_MODE_RECORD_ONLY.equals(replyPolicy.replyMode())) {
            conversationMapper.clearPendingReply(conversation.getId());
            updateRiskAndStatus(contact.getId(), conversation.getId(), AgentConstants.RISK_LEVEL_GREEN,
                    AgentConstants.CONVERSATION_STATUS_OPEN);
            return;
        }
        if (respectQuietWindow && shouldDelayReply(replyPolicy, inboundMessage)) {
            schedulePendingReply(conversation.getId(), inboundMessage, replyPolicy.quietSeconds());
            return;
        }
        AgentIntentAnalysis intentAnalysis = intentRecognitionService.recognize(agent, contact, inboundMessage);
        if (intentAnalysis == null) {
            intentAnalysis = AgentIntentAnalysis.none();
        }
        AgentReplyContext replyContext = contextBuilder.build(conversation.getId(), inboundMessage, agent, account);
        AgentGeneratedReply generatedReply = replyGenerator.generate(replyContext);
        List<AgentSensitiveRuleDO> sensitiveRules = selectApplicableSensitiveRules(account.getAgentId());
        if (!generatedReply.hasContent()) {
            if (!sensitiveRules.isEmpty() && intentAnalysis.needsHuman()) {
                AgentSensitiveRuleMatcher.MatchResult sensitiveMatch = builtInHumanTakeoverMatch(intentAnalysis);
                AgentReplyPolicyDecision policyDecision = autoReplyPolicyEvaluator.evaluate(replyPolicy, conversation,
                        generatedReply, sensitiveMatch, geweMessageClient.canSend(account), LocalDateTime.now(),
                        !respectQuietWindow);
                insertDecision(conversation, inboundMessage, null, null, sensitiveMatch, generatedReply, policyDecision,
                        replyContext, replyPolicy);
                updateRiskAndStatus(contact.getId(), conversation.getId(), policyDecision.riskLevel(),
                        policyDecision.conversationStatus());
                return;
            }
            recordNoGeneratedReplyDecision(contact, conversation, inboundMessage, generatedReply, replyContext, replyPolicy);
            updateRiskAndStatus(contact.getId(), conversation.getId(), AgentConstants.RISK_LEVEL_YELLOW,
                    AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
            return;
        }

        AgentSensitiveRuleMatcher.MatchResult sensitiveMatch = resolveSensitiveMatch(account.getAgentId(),
                inboundMessage.getContent(), generatedReply.content(), intentAnalysis, contact, replyContext, generatedReply,
                sensitiveRules);
        AgentReplyPolicyDecision policyDecision = autoReplyPolicyEvaluator.evaluate(replyPolicy, conversation, generatedReply,
                sensitiveMatch, geweMessageClient.canSend(account), LocalDateTime.now(), !respectQuietWindow);
        if (policyDecision.stopReply()) {
            if (sensitiveMatch.matched()) {
                insertDecision(conversation, inboundMessage, null, null, sensitiveMatch, generatedReply, policyDecision,
                        replyContext, replyPolicy);
            }
            updateRiskAndStatus(contact.getId(), conversation.getId(), policyDecision.riskLevel(),
                    policyDecision.conversationStatus());
            return;
        }
        if (!policyDecision.autoSend()) {
            insertPendingReviewMessage(account, contact, conversation, inboundMessage, generatedReply, sensitiveMatch,
                    policyDecision, replyContext, replyPolicy);
            updateRiskAndStatus(contact.getId(), conversation.getId(), policyDecision.riskLevel(),
                    policyDecision.conversationStatus());
            return;
        }

        sendAutoReply(account, contact, conversation, inboundMessage, generatedReply, sensitiveMatch, policyDecision,
                replyContext, replyPolicy);
    }

    @Override
    public void processDuePendingReplies(LocalDateTime now) {
        conversationMapper.selectDuePendingReplyList(now, 50).forEach(conversation -> processDuePendingReply(conversation, now));
    }

    private void processDuePendingReply(AgentConversationDO conversation, LocalDateTime now) {
        AgentMessageDO inboundMessage = messageMapper.selectById(conversation.getPendingReplyMessageId());
        if (inboundMessage == null || !Objects.equals(inboundMessage.getDirection(), AgentConstants.MESSAGE_DIRECTION_INBOUND)
                || !Objects.equals(conversation.getLastMessageId(), inboundMessage.getId())) {
            conversationMapper.clearPendingReply(conversation.getId());
            return;
        }
        AgentWechatAccountDO account = accountMapper.selectById(conversation.getWechatAccountId());
        AgentWechatContactDO contact = contactMapper.selectById(conversation.getContactId());
        if (account == null || contact == null || !Objects.equals(account.getStatus(), AgentConstants.STATUS_ENABLE)) {
            conversationMapper.clearPendingReply(conversation.getId());
            return;
        }
        conversation.setLastMessageTime(inboundMessage.getMessageTime());
        handleInboundMessage(account, contact, conversation, inboundMessage, false);
        log.debug("[processDuePendingReply][conversationId({}) pendingMessageId({}) processed at {}]",
                conversation.getId(), inboundMessage.getId(), now);
    }

    private boolean shouldDelayReply(AgentReplyPolicy replyPolicy, AgentMessageDO inboundMessage) {
        Integer quietSeconds = replyPolicy.quietSeconds();
        return quietSeconds != null && quietSeconds > 0 && inboundMessage.getMessageTime() != null;
    }

    private boolean isChatroomContact(AgentWechatContactDO contact) {
        return contact != null && StrUtil.endWith(StrUtil.trim(contact.getExternalUserId()), "@chatroom");
    }

    private boolean requiresHumanReviewForMedia(AgentMessageDO inboundMessage) {
        Integer messageType = inboundMessage.getMessageType();
        return Objects.equals(messageType, AgentConstants.MESSAGE_TYPE_IMAGE)
                || Objects.equals(messageType, AgentConstants.MESSAGE_TYPE_VOICE)
                || Objects.equals(messageType, AgentConstants.MESSAGE_TYPE_VIDEO)
                || Objects.equals(messageType, AgentConstants.MESSAGE_TYPE_EMOJI)
                || Objects.equals(messageType, AgentConstants.MESSAGE_TYPE_FILE_OR_LINK);
    }

    private void recordMediaManualDecision(AgentWechatContactDO contact, AgentConversationDO conversation,
                                           AgentMessageDO inboundMessage, AgentReplyPolicy replyPolicy) {
        AgentReplyContext replyContext = new AgentReplyContext(inboundMessage.getContent(), List.of(inboundMessage),
                List.of(), contact.getId(), resolveCustomerLevel(contact), List.of(), conversation.getAgentId(),
                null, "Customer sent media; human review required.", null, null);
        AgentGeneratedReply generatedReply = AgentGeneratedReply.blank();
        AgentReplyPolicyDecision decision = new AgentReplyPolicyDecision(false, false,
                AgentConstants.DECISION_TYPE_MANUAL_CONFIRM, AgentConstants.REVIEW_STATUS_PENDING,
                AgentConstants.RISK_LEVEL_YELLOW, AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM,
                "客户发送了图片、表情、文件或语音，需人工查看后处理，已暂停自动回复。");
        insertDecision(conversation, inboundMessage, null, null, AgentSensitiveRuleMatcher.MatchResult.none(),
                generatedReply, decision, replyContext, replyPolicy);
        updateRiskAndStatus(contact.getId(), conversation.getId(), decision.riskLevel(), decision.conversationStatus());
    }

    private void schedulePendingReply(Long conversationId, AgentMessageDO inboundMessage, Integer quietSeconds) {
        AgentConversationDO conversationUpdate = new AgentConversationDO();
        conversationUpdate.setId(conversationId);
        conversationUpdate.setStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        conversationUpdate.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        conversationUpdate.setPendingReplyMessageId(inboundMessage.getId());
        conversationUpdate.setPendingReplyDueTime(inboundMessage.getMessageTime().plusSeconds(quietSeconds));
        conversationUpdate.setContinuousAutoReplyCount(0);
        conversationMapper.updateById(conversationUpdate);
    }

    private AgentSensitiveRuleMatcher.MatchResult resolveSensitiveMatch(Long agentId, String inboundContent,
                                                                        String replyContent,
                                                                        AgentIntentAnalysis intentAnalysis,
                                                                        AgentWechatContactDO contact,
                                                                        AgentReplyContext replyContext,
                                                                        AgentGeneratedReply generatedReply,
                                                                        List<AgentSensitiveRuleDO> rules) {
        if (rules.isEmpty()) {
            return AgentSensitiveRuleMatcher.MatchResult.none();
        }
        boolean ragMiss = replyContext.knowledgeBaseId() != null
                && StrUtil.isBlank(generatedReply.matchedKnowledgeTitle());
        AgentRuleMatchContext inboundContext = new AgentRuleMatchContext(inboundContent, null, intentAnalysis,
                resolveCustomerLevel(contact),
                replyContext.customerTagNames(), ragMiss);
        AgentRuleMatchContext replyContextForRule = new AgentRuleMatchContext(inboundContent, replyContent, intentAnalysis,
                resolveCustomerLevel(contact),
                replyContext.customerTagNames(), ragMiss);
        AgentSensitiveRuleMatcher.MatchResult inboundMatch = sensitiveRuleMatcher.match(inboundContext, rules);
        AgentSensitiveRuleMatcher.MatchResult replyMatch = sensitiveRuleMatcher.match(replyContextForRule, rules);
        if (!inboundMatch.matched() && !replyMatch.matched() && intentAnalysis.needsHuman()) {
            return builtInHumanTakeoverMatch(intentAnalysis);
        }
        if (!inboundMatch.matched()) {
            return replyMatch;
        }
        if (!replyMatch.matched()) {
            return inboundMatch;
        }
        return nullToZero(replyMatch.riskLevel()) > nullToZero(inboundMatch.riskLevel()) ? replyMatch : inboundMatch;
    }

    private List<AgentSensitiveRuleDO> selectApplicableSensitiveRules(Long agentId) {
        List<AgentSensitiveRuleDO> rules = sensitiveRuleMapper.selectEnabledList();
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return rules.stream()
                .filter(rule -> rule.getAgentId() == null || Objects.equals(rule.getAgentId(), agentId))
                .filter(rule -> StrUtil.isBlank(rule.getRouteApp())
                        || AgentConstants.ROUTE_APP_GEWE.equalsIgnoreCase(rule.getRouteApp()))
                .toList();
    }

    private AgentSensitiveRuleMatcher.MatchResult builtInHumanTakeoverMatch(AgentIntentAnalysis intentAnalysis) {
        Integer riskLevel = intentAnalysis.riskLevel() == null
                ? AgentConstants.RISK_LEVEL_YELLOW : intentAnalysis.riskLevel();
        return new AgentSensitiveRuleMatcher.MatchResult(true,
                "\u5185\u7f6e\u9ad8\u98ce\u9669\u610f\u56fe",
                AgentConstants.SENSITIVE_ACTION_TAKEOVER,
                riskLevel,
                AgentConstants.ESCALATION_TRIGGER_REQUEST_HUMAN,
                intentAnalysis.intent(),
                intentAnalysis.reason(),
                intentAnalysis);
    }

    private Integer resolveCustomerLevel(AgentWechatContactDO contact) {
        return contact == null || contact.getCustomerLevel() == null
                ? AgentConstants.CUSTOMER_LEVEL_NORMAL : contact.getCustomerLevel();
    }

    private void recordNoGeneratedReplyDecision(AgentWechatContactDO contact, AgentConversationDO conversation,
                                                AgentMessageDO inboundMessage,
                                                AgentGeneratedReply generatedReply,
                                                AgentReplyContext replyContext,
                                                AgentReplyPolicy replyPolicy) {
        AgentReplyPolicyDecision decision = new AgentReplyPolicyDecision(false, false,
                AgentConstants.DECISION_TYPE_MANUAL_CONFIRM, AgentConstants.REVIEW_STATUS_PENDING,
                AgentConstants.RISK_LEVEL_YELLOW, AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM,
                "AI 没有生成可发送的回复，请人工查看客户问题后处理。常见原因：知识库未命中、知识库未配置，或模型返回为空。");
        insertDecision(conversation, inboundMessage, null, null, AgentSensitiveRuleMatcher.MatchResult.none(),
                generatedReply, decision, replyContext, replyPolicy);
        log.warn("[autoReplyNoContent][conversationId({}) contactId({}) inboundMessageId({}) policySource({})]",
                conversation.getId(), contact.getId(), inboundMessage.getId(), replyPolicy.source());
    }

    private void insertPendingReviewMessage(AgentWechatAccountDO account, AgentWechatContactDO contact,
                                            AgentConversationDO conversation, AgentMessageDO inboundMessage,
                                            AgentGeneratedReply generatedReply,
                                            AgentSensitiveRuleMatcher.MatchResult sensitiveMatch,
                                            AgentReplyPolicyDecision policyDecision, AgentReplyContext replyContext,
                                            AgentReplyPolicy replyPolicy) {
        AgentMessageDO message = buildAiMessage(account, contact, conversation, generatedReply);
        message.setSendStatus(AgentConstants.SEND_STATUS_PENDING_REVIEW);
        if (sensitiveMatch.matched()) {
            message.setMatchedPolicy(sensitiveMatch.ruleName());
        }
        message.setAuditNote(policyDecision.decisionReason());
        messageMapper.insert(message);
        insertDecision(conversation, inboundMessage, message, null, sensitiveMatch, generatedReply, policyDecision,
                replyContext, replyPolicy);
        updateConversationLastMessage(conversation.getId(), message.getId(), message.getMessageTime());
    }

    private void sendAutoReply(AgentWechatAccountDO account, AgentWechatContactDO contact,
                               AgentConversationDO conversation, AgentMessageDO inboundMessage,
                               AgentGeneratedReply generatedReply,
                               AgentSensitiveRuleMatcher.MatchResult sensitiveMatch,
                               AgentReplyPolicyDecision policyDecision, AgentReplyContext replyContext,
                               AgentReplyPolicy replyPolicy) {
        GeweTextSendResult sendResult = geweMessageClient.sendText(account, contact.getExternalUserId(),
                generatedReply.content());
        AgentMessageDO message = buildAiMessage(account, contact, conversation, generatedReply);
        message.setGeweMessageId(sendResult.geweMessageId());
        message.setRawPayload(sendResult.rawResponse());
        if (sendResult.success()) {
            message.setSendStatus(AgentConstants.SEND_STATUS_SENT);
            message.setAuditNote("自动回复已发送");
            messageMapper.insert(message);
            insertDecision(conversation, inboundMessage, message, message, sensitiveMatch, generatedReply,
                    policyDecision, replyContext, replyPolicy);
            updateConversationAfterSent(conversation, message.getId(), message.getMessageTime());
        } else {
            message.setSendStatus(AgentConstants.SEND_STATUS_FAILED);
            message.setAuditNote(StrUtil.maxLength(sendResult.errorMessage(), 512));
            messageMapper.insert(message);
            AgentReplyPolicyDecision fallbackDecision = new AgentReplyPolicyDecision(false, false,
                    AgentConstants.DECISION_TYPE_MANUAL_CONFIRM, AgentConstants.REVIEW_STATUS_PENDING,
                    AgentConstants.RISK_LEVEL_YELLOW, AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM,
                    "Gewe 发送失败，转人工确认");
            insertDecision(conversation, inboundMessage, message, null, AgentSensitiveRuleMatcher.MatchResult.none(),
                    generatedReply, fallbackDecision, replyContext, replyPolicy);
            updateRiskAndStatus(contact.getId(), conversation.getId(), AgentConstants.RISK_LEVEL_YELLOW,
                    AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
            updateConversationLastMessage(conversation.getId(), message.getId(), message.getMessageTime());
        }
    }

    private AgentMessageDO buildAiMessage(AgentWechatAccountDO account, AgentWechatContactDO contact,
                                          AgentConversationDO conversation, AgentGeneratedReply generatedReply) {
        AgentMessageDO message = new AgentMessageDO();
        message.setConversationId(conversation.getId());
        message.setWechatAccountId(account.getId());
        message.setContactId(contact.getId());
        message.setDirection(AgentConstants.MESSAGE_DIRECTION_OUTBOUND);
        message.setSenderType(AgentConstants.SENDER_AI_AGENT);
        message.setMessageType(AgentConstants.MESSAGE_TYPE_TEXT);
        message.setContent(generatedReply.content());
        message.setIntent("KNOWLEDGE_REPLY");
        message.setMatchedPolicy(generatedReply.matchedKnowledgeTitle());
        message.setRawPayload(buildKnowledgeRefs(generatedReply));
        message.setMessageTime(LocalDateTime.now());
        return message;
    }

    private void insertDecision(AgentConversationDO conversation, AgentMessageDO inboundMessage,
                                AgentMessageDO suggestedMessage, AgentMessageDO sentMessage,
                                AgentSensitiveRuleMatcher.MatchResult sensitiveMatch,
                                AgentGeneratedReply generatedReply, AgentReplyPolicyDecision policyDecision,
                                AgentReplyContext replyContext, AgentReplyPolicy replyPolicy) {
        AgentReplyDecisionDO decision = new AgentReplyDecisionDO();
        decision.setConversationId(conversation.getId());
        decision.setInboundMessageId(inboundMessage.getId());
        decision.setSuggestedMessageId(suggestedMessage == null ? null : suggestedMessage.getId());
        decision.setSentMessageId(sentMessage == null ? null : sentMessage.getId());
        decision.setDecisionType(policyDecision.decisionType());
        decision.setRiskLevel(policyDecision.riskLevel());
        decision.setConfidence(generatedReply.confidence());
        decision.setLlmModel(StrUtil.blankToDefault(generatedReply.llmModel(), "knowledge-deterministic"));
        decision.setPromptSnapshot(replyContext.systemPrompt());
        decision.setContextSnapshot(buildContextSnapshot(inboundMessage, conversation, replyContext, replyPolicy,
                generatedReply));
        decision.setKnowledgeRefs(buildKnowledgeRefs(generatedReply));
        decision.setGuardrailHits(buildGuardrailHits(sensitiveMatch));
        decision.setDecisionReason(policyDecision.decisionReason());
        decision.setReviewStatus(policyDecision.reviewStatus());
        replyDecisionMapper.insert(decision);
    }

    private Map<String, Object> buildContextSnapshot(AgentMessageDO inboundMessage, AgentConversationDO conversation,
                                                     AgentReplyContext replyContext, AgentReplyPolicy replyPolicy,
                                                     AgentGeneratedReply generatedReply) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("inboundContent", inboundMessage.getContent());
        snapshot.put("conversationId", conversation.getId());
        snapshot.put("continuousAutoReplyCount", conversation.getContinuousAutoReplyCount());
        snapshot.put("agentId", replyContext.agentId());
        snapshot.put("knowledgeBaseId", replyContext.knowledgeBaseId());
        snapshot.put("systemPrompt", replyContext.systemPrompt());
        snapshot.put("llmProvider", replyContext.llmProvider());
        snapshot.put("llmModel", replyContext.llmModel());
        snapshot.put("generationSource", generatedReply.generationSource());
        snapshot.put("actualLlmProvider", generatedReply.llmProvider());
        snapshot.put("actualLlmModel", generatedReply.llmModel());
        snapshot.put("knowledgeHits", buildKnowledgeHitRefs(replyContext));
        snapshot.put("contactId", replyContext.contactId());
        snapshot.put("customerLevel", replyContext.customerLevel());
        snapshot.put("customerTagNames", replyContext.customerTagNames());
        snapshot.put("replyPolicy", replyPolicy);
        snapshot.put("policySource", replyPolicy.source());
        return snapshot;
    }

    private List<Map<String, Object>> buildKnowledgeHitRefs(AgentReplyContext replyContext) {
        if (replyContext.knowledgeHits() == null || replyContext.knowledgeHits().isEmpty()) {
            return List.of();
        }
        return replyContext.knowledgeHits().stream()
                .limit(5)
                .map(hit -> {
                    Map<String, Object> ref = new HashMap<>();
                    ref.put("knowledgeBaseId", hit.knowledgeBaseId());
                    ref.put("knowledgeItemId", hit.knowledgeItemId());
                    ref.put("knowledgeChunkId", hit.knowledgeChunkId());
                    ref.put("title", hit.title());
                    ref.put("distance", hit.distance());
                    ref.put("keywordScore", hit.keywordScore());
                    return ref;
                })
                .toList();
    }

    private Map<String, Object> buildKnowledgeRefs(AgentGeneratedReply generatedReply) {
        Map<String, Object> refs = new HashMap<>();
        refs.put("generationSource", generatedReply.generationSource());
        if (StrUtil.isNotBlank(generatedReply.llmProvider())) {
            refs.put("llmProvider", generatedReply.llmProvider());
        }
        if (StrUtil.isNotBlank(generatedReply.llmModel())) {
            refs.put("llmModel", generatedReply.llmModel());
        }
        if (StrUtil.isNotBlank(generatedReply.matchedKnowledgeTitle())) {
            refs.put("matchedKnowledgeTitle", generatedReply.matchedKnowledgeTitle());
        }
        return refs;
    }

    private Map<String, Object> buildGuardrailHits(AgentSensitiveRuleMatcher.MatchResult sensitiveMatch) {
        Map<String, Object> hits = new HashMap<>();
        if (sensitiveMatch.matched()) {
            hits.put("ruleName", sensitiveMatch.ruleName());
            hits.put("action", sensitiveMatch.action());
            hits.put("riskLevel", sensitiveMatch.riskLevel());
            hits.put("triggerType", sensitiveMatch.triggerType());
            hits.put("triggerValue", sensitiveMatch.triggerValue());
            hits.put("reason", sensitiveMatch.reason());
            hits.put("intent", sensitiveMatch.intentAnalysis().intent());
            hits.put("sentiment", sensitiveMatch.intentAnalysis().sentiment());
            hits.put("intentConfidence", sensitiveMatch.intentAnalysis().confidence());
        }
        return hits;
    }

    private void updateRiskAndStatus(Long contactId, Long conversationId, Integer riskLevel, Integer status) {
        int normalizedRiskLevel = Math.max(nullToZero(riskLevel), AgentConstants.RISK_LEVEL_GREEN);
        AgentWechatContactDO contactUpdate = new AgentWechatContactDO();
        contactUpdate.setId(contactId);
        contactUpdate.setRiskLevel(normalizedRiskLevel);
        contactUpdate.setLastConversationStatus(status);
        contactMapper.updateById(contactUpdate);

        AgentConversationDO conversationUpdate = new AgentConversationDO();
        conversationUpdate.setId(conversationId);
        conversationUpdate.setRiskLevel(normalizedRiskLevel);
        conversationUpdate.setStatus(status);
        conversationUpdate.setContinuousAutoReplyCount(0);
        if (Objects.equals(status, AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER)) {
            conversationUpdate.setHumanTakeoverTime(LocalDateTime.now());
        }
        conversationMapper.updateById(conversationUpdate);
        conversationMapper.clearPendingReply(conversationId);
    }

    private void updateConversationAfterSent(AgentConversationDO conversation, Long messageId,
                                             LocalDateTime messageTime) {
        AgentConversationDO conversationUpdate = new AgentConversationDO();
        conversationUpdate.setId(conversation.getId());
        conversationUpdate.setStatus(AgentConstants.CONVERSATION_STATUS_AI_AUTO);
        conversationUpdate.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        conversationUpdate.setLastMessageId(messageId);
        conversationUpdate.setLastMessageTime(messageTime);
        conversationUpdate.setContinuousAutoReplyCount(
                conversation.getContinuousAutoReplyCount() == null ? 1 : conversation.getContinuousAutoReplyCount() + 1);
        conversationMapper.updateById(conversationUpdate);
        conversationMapper.clearPendingReply(conversation.getId());

        AgentWechatContactDO contactUpdate = new AgentWechatContactDO();
        contactUpdate.setId(conversation.getContactId());
        contactUpdate.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        contactUpdate.setLastConversationStatus(AgentConstants.CONVERSATION_STATUS_AI_AUTO);
        contactUpdate.setLastMessageTime(messageTime);
        contactMapper.updateById(contactUpdate);
    }

    private void updateConversationLastMessage(Long conversationId, Long messageId, LocalDateTime messageTime) {
        AgentConversationDO conversationUpdate = new AgentConversationDO();
        conversationUpdate.setId(conversationId);
        conversationUpdate.setLastMessageId(messageId);
        conversationUpdate.setLastMessageTime(messageTime);
        conversationMapper.updateById(conversationUpdate);
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

}
