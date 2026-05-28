package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAutoReplyPolicyEvaluatorTest {

    private final AgentAutoReplyPolicyEvaluator evaluator = new AgentAutoReplyPolicyEvaluator();

    @Test
    void evaluateIgnoresReplyConfidenceWhenAutoReplyIsOtherwiseAllowed() {
        AgentReplyPolicyDecision decision = evaluator.evaluate(autoPolicy(), conversation(0),
                reply("reply", "faq", "0.10"), AgentSensitiveRuleMatcher.MatchResult.none(),
                true, LocalDateTime.of(2026, 5, 19, 10, 0));

        assertThat(decision.autoSend()).isTrue();
        assertThat(decision.reviewStatus()).isEqualTo(AgentConstants.REVIEW_STATUS_SENT);
        assertThat(decision.conversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_AI_AUTO);
    }

    @Test
    void evaluateIgnoresContinuousAutoReplyCountWhenAutoReplyIsOtherwiseAllowed() {
        AgentReplyPolicyDecision decision = evaluator.evaluate(autoPolicy(), conversation(99),
                reply("reply", "faq", "0.95"), AgentSensitiveRuleMatcher.MatchResult.none(),
                true, LocalDateTime.of(2026, 5, 19, 10, 0));

        assertThat(decision.autoSend()).isTrue();
        assertThat(decision.conversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_AI_AUTO);
    }

    @Test
    void evaluateAllowsAutoReplyToRecoverOldHumanTakeoverStatus() {
        AgentConversationDO conversation = conversation(0);
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);

        AgentReplyPolicyDecision decision = evaluator.evaluate(autoPolicy(), conversation,
                reply("reply", "faq", "0.95"), AgentSensitiveRuleMatcher.MatchResult.none(),
                true, LocalDateTime.of(2026, 5, 19, 10, 0));

        assertThat(decision.stopReply()).isFalse();
        assertThat(decision.autoSend()).isTrue();
        assertThat(decision.conversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_AI_AUTO);
    }

    @Test
    void evaluateKeepsManualPolicyStoppedWhenConversationIsHumanTakeover() {
        AgentConversationDO conversation = conversation(0);
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
        AgentReplyPolicy manualPolicy = new AgentReplyPolicy(AgentConstants.REPLY_MODE_MANUAL_CONFIRM,
                0, Map.of(), "CONTACT");

        AgentReplyPolicyDecision decision = evaluator.evaluate(manualPolicy, conversation,
                reply("manual", "policy", "0.95"), AgentSensitiveRuleMatcher.MatchResult.none(),
                true, LocalDateTime.of(2026, 5, 19, 10, 0));

        assertThat(decision.stopReply()).isTrue();
        assertThat(decision.autoSend()).isFalse();
        assertThat(decision.conversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
    }

    @Test
    void evaluateStopsAutoReplyWhenTakeoverRuleMatches() {
        AgentSensitiveRuleMatcher.MatchResult redRule = new AgentSensitiveRuleMatcher.MatchResult(true,
                "refund escalation", AgentConstants.SENSITIVE_ACTION_TAKEOVER, AgentConstants.RISK_LEVEL_RED);

        AgentReplyPolicyDecision decision = evaluator.evaluate(autoPolicy(), conversation(0),
                reply("reply", "faq", "0.95"), redRule, true,
                LocalDateTime.of(2026, 5, 19, 10, 0));

        assertThat(decision.autoSend()).isFalse();
        assertThat(decision.stopReply()).isTrue();
        assertThat(decision.decisionType()).isEqualTo(AgentConstants.DECISION_TYPE_HUMAN_TAKEOVER);
        assertThat(decision.riskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_RED);
        assertThat(decision.conversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
    }

    @Test
    void evaluateStopsAutoReplyForManualReviewWhenReviewRuleMatches() {
        AgentSensitiveRuleMatcher.MatchResult reviewRule = new AgentSensitiveRuleMatcher.MatchResult(true,
                "contract review", AgentConstants.SENSITIVE_ACTION_REVIEW, AgentConstants.RISK_LEVEL_YELLOW);

        AgentReplyPolicyDecision decision = evaluator.evaluate(autoPolicy(), conversation(0),
                reply("reply", "faq", "0.95"), reviewRule, true,
                LocalDateTime.of(2026, 5, 19, 10, 0));

        assertThat(decision.autoSend()).isFalse();
        assertThat(decision.stopReply()).isFalse();
        assertThat(decision.decisionType()).isEqualTo(AgentConstants.DECISION_TYPE_MANUAL_CONFIRM);
        assertThat(decision.riskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_YELLOW);
        assertThat(decision.conversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
    }

    @Test
    void evaluateBlocksAutoReplyOutsideBusinessHours() {
        AgentReplyPolicy policy = new AgentReplyPolicy(AgentConstants.REPLY_MODE_AUTO_REPLY,
                0, Map.of("start", "09:00", "end", "18:00"), "ACCOUNT");

        AgentReplyPolicyDecision decision = evaluator.evaluate(policy, conversation(0),
                reply("reply", "faq", "0.95"), AgentSensitiveRuleMatcher.MatchResult.none(),
                true, LocalDateTime.of(2026, 5, 19, 20, 0));

        assertThat(decision.autoSend()).isFalse();
        assertThat(decision.conversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
    }

    @Test
    void evaluateBlocksAutoReplyWithinQuietSeconds() {
        AgentReplyPolicy policy = new AgentReplyPolicy(AgentConstants.REPLY_MODE_AUTO_REPLY,
                30, Map.of(), "ACCOUNT");
        AgentConversationDO conversation = conversation(0);
        conversation.setLastMessageTime(LocalDateTime.of(2026, 5, 19, 10, 0, 0));

        AgentReplyPolicyDecision decision = evaluator.evaluate(policy, conversation,
                reply("reply", "faq", "0.95"), AgentSensitiveRuleMatcher.MatchResult.none(),
                true, LocalDateTime.of(2026, 5, 19, 10, 0, 20));

        assertThat(decision.autoSend()).isFalse();
        assertThat(decision.conversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
    }

    @Test
    void evaluateAllowsDuePendingReplyWhenQuietWindowWasAlreadyHandled() {
        AgentReplyPolicy policy = new AgentReplyPolicy(AgentConstants.REPLY_MODE_AUTO_REPLY,
                30, Map.of(), "ACCOUNT");
        AgentConversationDO conversation = conversation(0);
        conversation.setLastMessageTime(LocalDateTime.of(2026, 5, 19, 10, 0, 0));

        AgentReplyPolicyDecision decision = evaluator.evaluate(policy, conversation,
                reply("reply", "faq", "0.95"), AgentSensitiveRuleMatcher.MatchResult.none(),
                true, LocalDateTime.of(2026, 5, 19, 10, 0, 20), true);

        assertThat(decision.autoSend()).isTrue();
        assertThat(decision.conversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_AI_AUTO);
    }

    private AgentReplyPolicy autoPolicy() {
        return new AgentReplyPolicy(AgentConstants.REPLY_MODE_AUTO_REPLY, 0, Map.of(), "ACCOUNT");
    }

    private AgentConversationDO conversation(Integer continuousAutoReplyCount) {
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        conversation.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        conversation.setContinuousAutoReplyCount(continuousAutoReplyCount);
        return conversation;
    }

    private AgentGeneratedReply reply(String content, String title, String confidence) {
        return new AgentGeneratedReply(content, title, new java.math.BigDecimal(confidence));
    }

}
