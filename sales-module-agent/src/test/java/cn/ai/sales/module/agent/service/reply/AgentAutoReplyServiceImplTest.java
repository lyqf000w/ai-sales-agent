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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentAutoReplyServiceImplTest {

    private final AgentMapper agentMapper = mock(AgentMapper.class);
    private final AgentSensitiveRuleMapper sensitiveRuleMapper = mock(AgentSensitiveRuleMapper.class);
    private final AgentMessageMapper messageMapper = mock(AgentMessageMapper.class);
    private final AgentReplyDecisionMapper replyDecisionMapper = mock(AgentReplyDecisionMapper.class);
    private final AgentConversationMapper conversationMapper = mock(AgentConversationMapper.class);
    private final AgentWechatAccountMapper accountMapper = mock(AgentWechatAccountMapper.class);
    private final AgentWechatContactMapper contactMapper = mock(AgentWechatContactMapper.class);
    private final AgentReplyGenerator replyGenerator = mock(AgentReplyGenerator.class);
    private final AgentConversationContextBuilder contextBuilder = mock(AgentConversationContextBuilder.class);
    private final AgentSensitiveRuleMatcher sensitiveRuleMatcher = mock(AgentSensitiveRuleMatcher.class);
    private final GeweMessageClient geweMessageClient = mock(GeweMessageClient.class);
    private final AgentAutoReplyPolicyEvaluator autoReplyPolicyEvaluator = mock(AgentAutoReplyPolicyEvaluator.class);
    private final AgentReplyPolicyResolver replyPolicyResolver = mock(AgentReplyPolicyResolver.class);
    private final AgentIntentRecognitionService intentRecognitionService = mock(AgentIntentRecognitionService.class);
    private final AgentAutoReplyServiceImpl service = newService();

    @Test
    void handleInboundMessageDoesNotGenerateSuggestionWhenPolicyIsRecordOnly() {
        AgentWechatAccountDO account = account(1L, 2L);
        AgentWechatContactDO contact = contact(3L);
        AgentConversationDO conversation = conversation(4L, 3L);
        AgentMessageDO inbound = inbound(5L, 4L, "enterprise price", LocalDateTime.of(2026, 5, 20, 10, 0));
        AgentDO agent = enabledAgent(2L);

        when(agentMapper.selectById(2L)).thenReturn(agent);
        when(replyPolicyResolver.resolve(account, contact)).thenReturn(new AgentReplyPolicy(
                AgentConstants.REPLY_MODE_RECORD_ONLY, 0, Map.of(), "ACCOUNT"));

        service.handleInboundMessage(account, contact, conversation, inbound);

        verify(contextBuilder, never()).build(conversation.getId(), inbound, agent, account);
        verify(replyGenerator, never()).generate(any());
        verify(messageMapper, never()).insert(any(AgentMessageDO.class));
    }

    @Test
    void handleInboundMessageIgnoresChatroomConversations() {
        AgentWechatAccountDO account = account(1L, 2L);
        AgentWechatContactDO contact = contact(3L);
        contact.setExternalUserId("20666784639@chatroom");
        AgentConversationDO conversation = conversation(4L, 3L);
        AgentMessageDO inbound = inbound(5L, 4L, "wxid_member:\nhello in group",
                LocalDateTime.of(2026, 5, 20, 10, 0));

        service.handleInboundMessage(account, contact, conversation, inbound);

        verify(agentMapper, never()).selectById(any());
        verify(contextBuilder, never()).build(any(), any(), any(), any());
        verify(replyGenerator, never()).generate(any());
        verify(geweMessageClient, never()).sendText(any(), any(), any());
        verify(messageMapper, never()).insert(any(AgentMessageDO.class));
        verify(conversationMapper).clearPendingReply(4L);
    }

    @Test
    void handleInboundMessageSchedulesPendingReplyDuringQuietWindow() {
        AgentWechatAccountDO account = account(1L, 2L);
        AgentWechatContactDO contact = contact(3L);
        AgentConversationDO conversation = conversation(4L, 3L);
        AgentMessageDO inbound = inbound(5L, 4L, "need several answers", LocalDateTime.of(2026, 5, 20, 10, 0));
        AgentDO agent = enabledAgent(2L);

        when(agentMapper.selectById(2L)).thenReturn(agent);
        when(replyPolicyResolver.resolve(account, contact)).thenReturn(new AgentReplyPolicy(
                AgentConstants.REPLY_MODE_AUTO_REPLY, 60, Map.of("start", "08:00", "end", "22:00"), "ACCOUNT"));

        service.handleInboundMessage(account, contact, conversation, inbound);

        ArgumentCaptor<AgentConversationDO> captor = ArgumentCaptor.forClass(AgentConversationDO.class);
        verify(conversationMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPendingReplyMessageId()).isEqualTo(5L);
        assertThat(captor.getValue().getPendingReplyDueTime())
                .isEqualTo(LocalDateTime.of(2026, 5, 20, 10, 1, 0));
        verify(contextBuilder, never()).build(conversation.getId(), inbound, agent, account);
        verify(replyGenerator, never()).generate(any());
        verify(messageMapper, never()).insert(any(AgentMessageDO.class));
    }

    @Test
    void processDuePendingRepliesGeneratesReplyForLatestCustomerMessageAfterQuietWindow() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 20, 10, 1, 1);
        AgentConversationDO conversation = conversation(4L, 3L);
        conversation.setWechatAccountId(1L);
        conversation.setLastMessageId(5L);
        conversation.setPendingReplyMessageId(5L);
        conversation.setPendingReplyDueTime(LocalDateTime.of(2026, 5, 20, 10, 1, 0));
        AgentMessageDO inbound = inbound(5L, 4L, "please answer together", LocalDateTime.of(2026, 5, 20, 10, 0));
        AgentWechatAccountDO account = account(1L, 2L);
        account.setStatus(AgentConstants.STATUS_ENABLE);
        AgentWechatContactDO contact = contact(3L);
        contact.setExternalUserId("wxid_customer");
        AgentDO agent = enabledAgent(2L);
        AgentReplyContext context = new AgentReplyContext(inbound.getContent(), List.of(inbound), List.of());

        when(conversationMapper.selectDuePendingReplyList(now, 50)).thenReturn(List.of(conversation));
        when(messageMapper.selectById(5L)).thenReturn(inbound);
        when(accountMapper.selectById(1L)).thenReturn(account);
        when(contactMapper.selectById(3L)).thenReturn(contact);
        when(agentMapper.selectById(2L)).thenReturn(agent);
        when(replyPolicyResolver.resolve(account, contact)).thenReturn(new AgentReplyPolicy(
                AgentConstants.REPLY_MODE_AUTO_REPLY, 60, Map.of("start", "08:00", "end", "22:00"), "ACCOUNT"));
        when(contextBuilder.build(4L, inbound, agent, account)).thenReturn(context);
        when(replyGenerator.generate(context)).thenReturn(AgentGeneratedReply.llm("merged reply", "product faq",
                null, "DEEPSEEK", "deepseek-v4-pro"));
        when(intentRecognitionService.recognize(agent, contact, inbound)).thenReturn(AgentIntentAnalysis.none());
        when(sensitiveRuleMapper.selectEnabledList()).thenReturn(List.of());
        when(sensitiveRuleMatcher.match(any(AgentRuleMatchContext.class), any()))
                .thenReturn(AgentSensitiveRuleMatcher.MatchResult.none());
        when(geweMessageClient.canSend(account)).thenReturn(true);
        when(autoReplyPolicyEvaluator.evaluate(any(), any(), any(), any(), anyBoolean(), any(), anyBoolean()))
                .thenReturn(new AgentReplyPolicyDecision(false, true, AgentConstants.DECISION_TYPE_AUTO_SEND,
                        AgentConstants.REVIEW_STATUS_SENT, AgentConstants.RISK_LEVEL_GREEN,
                        AgentConstants.CONVERSATION_STATUS_AI_AUTO, "auto reply allowed"));
        when(geweMessageClient.sendText(account, "wxid_customer", "merged reply"))
                .thenReturn(GeweTextSendResult.success("gewe-msg-1", Map.of()));

        service.processDuePendingReplies(now);

        verify(contextBuilder).build(4L, inbound, agent, account);
        verify(replyGenerator).generate(context);
        verify(messageMapper).insert(any(AgentMessageDO.class));
        ArgumentCaptor<AgentReplyDecisionDO> decisionCaptor = ArgumentCaptor.forClass(AgentReplyDecisionDO.class);
        verify(replyDecisionMapper).insert(decisionCaptor.capture());
        AgentReplyDecisionDO decision = decisionCaptor.getValue();
        assertThat(decision.getLlmModel()).isEqualTo("deepseek-v4-pro");
        assertThat(decision.getKnowledgeRefs()).containsEntry("generationSource", AgentGeneratedReply.SOURCE_DEEPSEEK);
        assertThat(decision.getKnowledgeRefs()).containsEntry("llmProvider", "DEEPSEEK");
        assertThat(decision.getKnowledgeRefs()).containsEntry("llmModel", "deepseek-v4-pro");
        assertThat(decision.getKnowledgeRefs()).containsEntry("matchedKnowledgeTitle", "product faq");
        verify(conversationMapper).clearPendingReply(4L);
    }

    @Test
    void handleInboundMessageRecordsDecisionWhenSensitiveRuleTakesOver() {
        AgentWechatAccountDO account = account(1L, 2L);
        AgentWechatContactDO contact = contact(3L);
        contact.setExternalUserId("wxid_customer");
        AgentConversationDO conversation = conversation(4L, 3L);
        AgentMessageDO inbound = inbound(5L, 4L, "refund complaint", LocalDateTime.of(2026, 5, 20, 10, 0));
        AgentDO agent = enabledAgent(2L);
        AgentReplyContext context = new AgentReplyContext(inbound.getContent(), List.of(inbound), List.of());
        AgentGeneratedReply reply = AgentGeneratedReply.llm("I will record this", null, null,
                "DEEPSEEK", "deepseek-chat");
        AgentIntentAnalysis intent = new AgentIntentAnalysis("refund_or_complaint", "negative", true,
                AgentConstants.RISK_LEVEL_RED, "0.95", "refund complaint");
        AgentSensitiveRuleDO rule = new AgentSensitiveRuleDO();
        rule.setAgentId(2L);
        rule.setRouteApp(AgentConstants.ROUTE_APP_GEWE);
        rule.setName("refund complaint");
        rule.setTriggerType(AgentConstants.ESCALATION_TRIGGER_INTENT);
        rule.setPattern("refund_or_complaint");
        rule.setAction(AgentConstants.SENSITIVE_ACTION_TAKEOVER);
        rule.setRiskLevel(AgentConstants.RISK_LEVEL_RED);
        rule.setStatus(AgentConstants.STATUS_ENABLE);
        AgentSensitiveRuleMatcher.MatchResult takeoverMatch = new AgentSensitiveRuleMatcher.MatchResult(true,
                "refund complaint", AgentConstants.SENSITIVE_ACTION_TAKEOVER, AgentConstants.RISK_LEVEL_RED,
                AgentConstants.ESCALATION_TRIGGER_INTENT, "refund_or_complaint", "refund complaint", intent);

        when(agentMapper.selectById(2L)).thenReturn(agent);
        when(replyPolicyResolver.resolve(account, contact)).thenReturn(new AgentReplyPolicy(
                AgentConstants.REPLY_MODE_AUTO_REPLY, 0, Map.of("start", "08:00", "end", "22:00"), "ACCOUNT"));
        when(contextBuilder.build(4L, inbound, agent, account)).thenReturn(context);
        when(replyGenerator.generate(context)).thenReturn(reply);
        when(intentRecognitionService.recognize(agent, contact, inbound)).thenReturn(intent);
        when(sensitiveRuleMapper.selectEnabledList()).thenReturn(List.of(rule));
        when(sensitiveRuleMatcher.match(any(AgentRuleMatchContext.class), any()))
                .thenReturn(takeoverMatch, AgentSensitiveRuleMatcher.MatchResult.none());
        when(geweMessageClient.canSend(account)).thenReturn(true);
        when(autoReplyPolicyEvaluator.evaluate(any(), any(), any(), any(), anyBoolean(), any(), anyBoolean()))
                .thenReturn(new AgentReplyPolicyDecision(true, false, AgentConstants.DECISION_TYPE_HUMAN_TAKEOVER,
                        AgentConstants.REVIEW_STATUS_PENDING, AgentConstants.RISK_LEVEL_RED,
                        AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER, "manual takeover"));

        service.handleInboundMessage(account, contact, conversation, inbound);

        verify(geweMessageClient, never()).sendText(any(), any(), any());
        verify(messageMapper, never()).insert(any(AgentMessageDO.class));
        ArgumentCaptor<AgentReplyDecisionDO> decisionCaptor = ArgumentCaptor.forClass(AgentReplyDecisionDO.class);
        verify(replyDecisionMapper).insert(decisionCaptor.capture());
        AgentReplyDecisionDO decision = decisionCaptor.getValue();
        assertThat(decision.getConversationId()).isEqualTo(4L);
        assertThat(decision.getInboundMessageId()).isEqualTo(5L);
        assertThat(decision.getDecisionType()).isEqualTo(AgentConstants.DECISION_TYPE_HUMAN_TAKEOVER);
        assertThat(decision.getRiskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_RED);
        assertThat(decision.getGuardrailHits()).containsEntry("ruleName", "refund complaint");
        assertThat(decision.getGuardrailHits()).containsEntry("action", AgentConstants.SENSITIVE_ACTION_TAKEOVER);
        verify(conversationMapper).clearPendingReply(4L);
    }

    @Test
    void handleInboundMessageRecordsDecisionWhenReplyGeneratorReturnsBlank() {
        AgentWechatAccountDO account = account(1L, 2L);
        AgentWechatContactDO contact = contact(3L);
        AgentConversationDO conversation = conversation(4L, 3L);
        AgentMessageDO inbound = inbound(5L, 4L, "hello", LocalDateTime.of(2026, 5, 20, 10, 0));
        AgentDO agent = enabledAgent(2L);
        AgentReplyPolicy policy = new AgentReplyPolicy(AgentConstants.REPLY_MODE_AUTO_REPLY,
                0, Map.of("start", "08:00", "end", "22:00"), "CONTACT");
        AgentReplyContext context = new AgentReplyContext(inbound.getContent(), List.of(inbound), List.of());

        when(agentMapper.selectById(2L)).thenReturn(agent);
        when(replyPolicyResolver.resolve(account, contact)).thenReturn(policy);
        when(contextBuilder.build(4L, inbound, agent, account)).thenReturn(context);
        when(replyGenerator.generate(context)).thenReturn(AgentGeneratedReply.blank());

        service.handleInboundMessage(account, contact, conversation, inbound);

        verify(geweMessageClient, never()).sendText(any(), any(), any());
        verify(messageMapper, never()).insert(any(AgentMessageDO.class));
        ArgumentCaptor<AgentReplyDecisionDO> decisionCaptor = ArgumentCaptor.forClass(AgentReplyDecisionDO.class);
        verify(replyDecisionMapper).insert(decisionCaptor.capture());
        AgentReplyDecisionDO decision = decisionCaptor.getValue();
        assertThat(decision.getConversationId()).isEqualTo(4L);
        assertThat(decision.getInboundMessageId()).isEqualTo(5L);
        assertThat(decision.getDecisionType()).isEqualTo(AgentConstants.DECISION_TYPE_MANUAL_CONFIRM);
        assertThat(decision.getRiskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_YELLOW);
        assertThat(decision.getReviewStatus()).isEqualTo(AgentConstants.REVIEW_STATUS_PENDING);
        assertThat(decision.getDecisionReason()).contains("AI 没有生成可发送的回复");
        assertThat(decision.getKnowledgeRefs()).containsEntry("generationSource", AgentGeneratedReply.SOURCE_NONE);
        verify(conversationMapper).clearPendingReply(4L);
    }

    @Test
    void handleInboundMediaMessageRequiresHumanReviewBeforeDeepSeek() {
        AgentWechatAccountDO account = account(1L, 2L);
        AgentWechatContactDO contact = contact(3L);
        contact.setExternalUserId("wxid_customer");
        AgentConversationDO conversation = conversation(4L, 3L);
        AgentMessageDO inbound = inbound(5L, 4L, "<msg><img /></msg>", LocalDateTime.of(2026, 5, 20, 10, 0));
        inbound.setMessageType(AgentConstants.MESSAGE_TYPE_IMAGE);
        AgentDO agent = enabledAgent(2L);
        AgentReplyPolicy policy = new AgentReplyPolicy(AgentConstants.REPLY_MODE_AUTO_REPLY,
                30, Map.of("start", "08:00", "end", "22:00"), "CONTACT");

        when(agentMapper.selectById(2L)).thenReturn(agent);
        when(replyPolicyResolver.resolve(account, contact)).thenReturn(policy);

        service.handleInboundMessage(account, contact, conversation, inbound);

        verify(contextBuilder, never()).build(any(), any(), any(), any());
        verify(replyGenerator, never()).generate(any());
        verify(geweMessageClient, never()).sendText(any(), any(), any());
        verify(messageMapper, never()).insert(any(AgentMessageDO.class));

        ArgumentCaptor<AgentReplyDecisionDO> decisionCaptor = ArgumentCaptor.forClass(AgentReplyDecisionDO.class);
        verify(replyDecisionMapper).insert(decisionCaptor.capture());
        AgentReplyDecisionDO decision = decisionCaptor.getValue();
        assertThat(decision.getConversationId()).isEqualTo(4L);
        assertThat(decision.getInboundMessageId()).isEqualTo(5L);
        assertThat(decision.getDecisionType()).isEqualTo(AgentConstants.DECISION_TYPE_MANUAL_CONFIRM);
        assertThat(decision.getReviewStatus()).isEqualTo(AgentConstants.REVIEW_STATUS_PENDING);
        assertThat(decision.getRiskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_YELLOW);
        assertThat(decision.getDecisionReason()).contains("人工查看");

        ArgumentCaptor<AgentConversationDO> conversationCaptor = ArgumentCaptor.forClass(AgentConversationDO.class);
        verify(conversationMapper).updateById(conversationCaptor.capture());
        assertThat(conversationCaptor.getValue().getStatus())
                .isEqualTo(AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
        verify(conversationMapper).clearPendingReply(4L);
    }

    @Test
    void handleChatroomMediaMessageRequiresHumanReview() {
        AgentWechatAccountDO account = account(1L, 2L);
        AgentWechatContactDO contact = contact(3L);
        contact.setExternalUserId("20666784639@chatroom");
        AgentConversationDO conversation = conversation(4L, 3L);
        AgentMessageDO inbound = inbound(5L, 4L, "<msg><emoji /></msg>",
                LocalDateTime.of(2026, 5, 20, 10, 0));
        inbound.setMessageType(AgentConstants.MESSAGE_TYPE_EMOJI);
        AgentDO agent = enabledAgent(2L);

        when(agentMapper.selectById(2L)).thenReturn(agent);
        when(replyPolicyResolver.resolve(account, contact)).thenReturn(new AgentReplyPolicy(
                AgentConstants.REPLY_MODE_AUTO_REPLY, 0, Map.of("start", "08:00", "end", "22:00"), "ACCOUNT"));

        service.handleInboundMessage(account, contact, conversation, inbound);

        verify(replyDecisionMapper).insert(any(AgentReplyDecisionDO.class));
        verify(replyGenerator, never()).generate(any());
        ArgumentCaptor<AgentConversationDO> conversationCaptor = ArgumentCaptor.forClass(AgentConversationDO.class);
        verify(conversationMapper).updateById(conversationCaptor.capture());
        assertThat(conversationCaptor.getValue().getStatus())
                .isEqualTo(AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
    }

    @Test
    void handleInboundMessageDoesNotFallbackToBuiltInTakeoverWhenNoRuleExists() {
        AgentWechatAccountDO account = account(1L, 2L);
        AgentWechatContactDO contact = contact(3L);
        contact.setExternalUserId("wxid_customer");
        AgentConversationDO conversation = conversation(4L, 3L);
        AgentMessageDO inbound = inbound(5L, 4L, "I want to complain", LocalDateTime.of(2026, 5, 20, 10, 0));
        AgentDO agent = enabledAgent(2L);
        AgentReplyContext context = new AgentReplyContext(inbound.getContent(), List.of(inbound), List.of());
        AgentGeneratedReply reply = AgentGeneratedReply.llm("I will record this", null, null,
                "DEEPSEEK", "deepseek-chat");
        AgentIntentAnalysis intent = new AgentIntentAnalysis("refund_or_complaint", "negative", true,
                AgentConstants.RISK_LEVEL_RED, "0.95", "complaint intent");

        when(agentMapper.selectById(2L)).thenReturn(agent);
        when(replyPolicyResolver.resolve(account, contact)).thenReturn(new AgentReplyPolicy(
                AgentConstants.REPLY_MODE_AUTO_REPLY, 0, Map.of("start", "08:00", "end", "22:00"), "ACCOUNT"));
        when(contextBuilder.build(4L, inbound, agent, account)).thenReturn(context);
        when(replyGenerator.generate(context)).thenReturn(reply);
        when(intentRecognitionService.recognize(agent, contact, inbound)).thenReturn(intent);
        when(sensitiveRuleMapper.selectEnabledList()).thenReturn(List.of());
        when(sensitiveRuleMatcher.match(any(AgentRuleMatchContext.class), any()))
                .thenReturn(AgentSensitiveRuleMatcher.MatchResult.none());
        when(geweMessageClient.canSend(account)).thenReturn(true);
        when(autoReplyPolicyEvaluator.evaluate(any(), any(), any(), any(), anyBoolean(), any(), anyBoolean()))
                .thenReturn(new AgentReplyPolicyDecision(false, true, AgentConstants.DECISION_TYPE_AUTO_SEND,
                        AgentConstants.REVIEW_STATUS_SENT, AgentConstants.RISK_LEVEL_GREEN,
                        AgentConstants.CONVERSATION_STATUS_AI_AUTO, "auto reply allowed"));
        when(geweMessageClient.sendText(account, "wxid_customer", "I will record this"))
                .thenReturn(GeweTextSendResult.success("gewe-msg-1", Map.of()));

        service.handleInboundMessage(account, contact, conversation, inbound);

        ArgumentCaptor<AgentSensitiveRuleMatcher.MatchResult> matchCaptor =
                ArgumentCaptor.forClass(AgentSensitiveRuleMatcher.MatchResult.class);
        verify(autoReplyPolicyEvaluator).evaluate(any(), any(), any(), matchCaptor.capture(), anyBoolean(), any(), anyBoolean());
        assertThat(matchCaptor.getValue().matched()).isFalse();
        verify(geweMessageClient).sendText(account, "wxid_customer", "I will record this");
    }

    @Test
    void handleInboundMessageUsesBuiltInTakeoverWhenHumanQuestionMissesConfiguredRule() {
        AgentWechatAccountDO account = account(1L, 2L);
        AgentWechatContactDO contact = contact(3L);
        contact.setExternalUserId("wxid_customer");
        AgentConversationDO conversation = conversation(4L, 3L);
        AgentMessageDO inbound = inbound(5L, 4L, "你是真人吗？", LocalDateTime.of(2026, 5, 20, 10, 0));
        AgentDO agent = enabledAgent(2L);
        AgentReplyContext context = new AgentReplyContext(inbound.getContent(), List.of(inbound), List.of());
        AgentGeneratedReply reply = AgentGeneratedReply.llm("我是 AI 销售助手，可以先帮你解答产品问题。", null,
                null, "DEEPSEEK", "deepseek-chat");
        AgentIntentAnalysis intent = new AgentIntentAnalysis("request_human", "neutral", true,
                AgentConstants.RISK_LEVEL_YELLOW, "0.90", "客户询问是否真人");
        AgentSensitiveRuleDO rule = new AgentSensitiveRuleDO();
        rule.setAgentId(2L);
        rule.setRouteApp(AgentConstants.ROUTE_APP_GEWE);
        rule.setName("投诉退款");
        rule.setTriggerType(AgentConstants.ESCALATION_TRIGGER_KEYWORD);
        rule.setPattern("投诉退款");
        rule.setAction(AgentConstants.SENSITIVE_ACTION_TAKEOVER);
        rule.setRiskLevel(AgentConstants.RISK_LEVEL_RED);
        rule.setStatus(AgentConstants.STATUS_ENABLE);

        when(agentMapper.selectById(2L)).thenReturn(agent);
        when(replyPolicyResolver.resolve(account, contact)).thenReturn(new AgentReplyPolicy(
                AgentConstants.REPLY_MODE_AUTO_REPLY, 0, Map.of("start", "08:00", "end", "22:00"), "ACCOUNT"));
        when(contextBuilder.build(4L, inbound, agent, account)).thenReturn(context);
        when(replyGenerator.generate(context)).thenReturn(reply);
        when(intentRecognitionService.recognize(agent, contact, inbound)).thenReturn(intent);
        when(sensitiveRuleMapper.selectEnabledList()).thenReturn(List.of(rule));
        when(sensitiveRuleMatcher.match(any(AgentRuleMatchContext.class), any()))
                .thenReturn(AgentSensitiveRuleMatcher.MatchResult.none(), AgentSensitiveRuleMatcher.MatchResult.none());
        when(geweMessageClient.canSend(account)).thenReturn(true);
        when(autoReplyPolicyEvaluator.evaluate(any(), any(), any(), any(), anyBoolean(), any(), anyBoolean()))
                .thenReturn(new AgentReplyPolicyDecision(true, false, AgentConstants.DECISION_TYPE_HUMAN_TAKEOVER,
                        AgentConstants.REVIEW_STATUS_PENDING, AgentConstants.RISK_LEVEL_YELLOW,
                        AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM, "built-in high risk intent"));
        service.handleInboundMessage(account, contact, conversation, inbound);

        ArgumentCaptor<AgentSensitiveRuleMatcher.MatchResult> matchCaptor =
                ArgumentCaptor.forClass(AgentSensitiveRuleMatcher.MatchResult.class);
        verify(autoReplyPolicyEvaluator).evaluate(any(), any(), any(), matchCaptor.capture(), anyBoolean(), any(), anyBoolean());
        assertThat(matchCaptor.getValue().matched()).isTrue();
        assertThat(matchCaptor.getValue().ruleName()).isEqualTo("\u5185\u7f6e\u9ad8\u98ce\u9669\u610f\u56fe");
        assertThat(matchCaptor.getValue().action()).isEqualTo(AgentConstants.SENSITIVE_ACTION_TAKEOVER);
        verify(replyDecisionMapper).insert(any(AgentReplyDecisionDO.class));
        verify(geweMessageClient, never()).sendText(any(), any(), any());
    }

    private AgentWechatAccountDO account(Long id, Long agentId) {
        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setId(id);
        account.setAgentId(agentId);
        return account;
    }

    private AgentWechatContactDO contact(Long id) {
        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(id);
        return contact;
    }

    private AgentConversationDO conversation(Long id, Long contactId) {
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(id);
        conversation.setContactId(contactId);
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        conversation.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        conversation.setContinuousAutoReplyCount(0);
        return conversation;
    }

    private AgentMessageDO inbound(Long id, Long conversationId, String content, LocalDateTime messageTime) {
        AgentMessageDO inbound = new AgentMessageDO();
        inbound.setId(id);
        inbound.setConversationId(conversationId);
        inbound.setDirection(AgentConstants.MESSAGE_DIRECTION_INBOUND);
        inbound.setContent(content);
        inbound.setMessageTime(messageTime);
        return inbound;
    }

    private AgentDO enabledAgent(Long id) {
        AgentDO agent = new AgentDO();
        agent.setId(id);
        agent.setStatus(AgentConstants.STATUS_ENABLE);
        return agent;
    }

    private AgentAutoReplyServiceImpl newService() {
        AgentAutoReplyServiceImpl service = new AgentAutoReplyServiceImpl();
        ReflectionTestUtils.setField(service, "agentMapper", agentMapper);
        ReflectionTestUtils.setField(service, "sensitiveRuleMapper", sensitiveRuleMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "replyDecisionMapper", replyDecisionMapper);
        ReflectionTestUtils.setField(service, "conversationMapper", conversationMapper);
        ReflectionTestUtils.setField(service, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(service, "contactMapper", contactMapper);
        ReflectionTestUtils.setField(service, "replyGenerator", replyGenerator);
        ReflectionTestUtils.setField(service, "contextBuilder", contextBuilder);
        ReflectionTestUtils.setField(service, "sensitiveRuleMatcher", sensitiveRuleMatcher);
        ReflectionTestUtils.setField(service, "geweMessageClient", geweMessageClient);
        ReflectionTestUtils.setField(service, "autoReplyPolicyEvaluator", autoReplyPolicyEvaluator);
        ReflectionTestUtils.setField(service, "replyPolicyResolver", replyPolicyResolver);
        ReflectionTestUtils.setField(service, "intentRecognitionService", intentRecognitionService);
        return service;
    }
}
