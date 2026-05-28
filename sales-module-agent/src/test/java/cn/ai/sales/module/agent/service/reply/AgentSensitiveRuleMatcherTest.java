package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentSensitiveRuleDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSensitiveRuleMatcherTest {

    private final AgentSensitiveRuleMatcher matcher = new AgentSensitiveRuleMatcher();

    @Test
    void matchUsesIntentAnalysisForIntentRules() {
        AgentSensitiveRuleDO rule = rule("退款意图升级", AgentConstants.ESCALATION_TRIGGER_INTENT,
                "refund_request", AgentConstants.SENSITIVE_ACTION_TAKEOVER, AgentConstants.RISK_LEVEL_RED);
        AgentIntentAnalysis intent = new AgentIntentAnalysis("refund_request", "angry", true,
                AgentConstants.RISK_LEVEL_RED, "0.88", "客户提出退款");

        AgentSensitiveRuleMatcher.MatchResult result = matcher.match(new AgentRuleMatchContext(
                "我要退款", "", intent, AgentConstants.CUSTOMER_LEVEL_NORMAL, List.of(), false), List.of(rule));

        assertThat(result.matched()).isTrue();
        assertThat(result.ruleName()).isEqualTo("退款意图升级");
        assertThat(result.action()).isEqualTo(AgentConstants.SENSITIVE_ACTION_TAKEOVER);
        assertThat(result.riskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_RED);
        assertThat(result.triggerType()).isEqualTo(AgentConstants.ESCALATION_TRIGGER_INTENT);
        assertThat(result.intentAnalysis().intent()).isEqualTo("refund_request");
    }

    @Test
    void matchUsesRagMissFlag() {
        AgentSensitiveRuleDO rule = rule("知识库未命中", AgentConstants.ESCALATION_TRIGGER_RAG_MISS,
                "true", AgentConstants.SENSITIVE_ACTION_REVIEW, AgentConstants.RISK_LEVEL_YELLOW);

        AgentSensitiveRuleMatcher.MatchResult result = matcher.match(new AgentRuleMatchContext(
                "专业问题", "建议回复", AgentIntentAnalysis.none(),
                AgentConstants.CUSTOMER_LEVEL_NORMAL, List.of(), true), List.of(rule));

        assertThat(result.matched()).isTrue();
        assertThat(result.ruleName()).isEqualTo("知识库未命中");
    }

    @Test
    void matchUsesKeywordAgainstInboundAndReplyContent() {
        AgentSensitiveRuleDO rule = rule("金额承诺关键词", AgentConstants.ESCALATION_TRIGGER_KEYWORD,
                "最低价,赔付", AgentConstants.SENSITIVE_ACTION_REVIEW, AgentConstants.RISK_LEVEL_YELLOW);

        AgentSensitiveRuleMatcher.MatchResult result = matcher.match(new AgentRuleMatchContext(
                "这个是不是最低价", "", AgentIntentAnalysis.none(),
                AgentConstants.CUSTOMER_LEVEL_NORMAL, List.of(), false), List.of(rule));

        assertThat(result.matched()).isTrue();
        assertThat(result.triggerType()).isEqualTo(AgentConstants.ESCALATION_TRIGGER_KEYWORD);
    }

    @Test
    void matchUsesConfiguredRefundComplaintKeywords() {
        AgentSensitiveRuleDO rule = rule("投诉退款", AgentConstants.ESCALATION_TRIGGER_KEYWORD,
                "投诉退款,投诉,退款", AgentConstants.SENSITIVE_ACTION_TAKEOVER, AgentConstants.RISK_LEVEL_RED);

        AgentSensitiveRuleMatcher.MatchResult result = matcher.match(new AgentRuleMatchContext(
                "我要投诉退款，马上让人工处理", "", AgentIntentAnalysis.none(),
                AgentConstants.CUSTOMER_LEVEL_NORMAL, List.of(), false), List.of(rule));

        assertThat(result.matched()).isTrue();
        assertThat(result.ruleName()).isEqualTo("投诉退款");
        assertThat(result.action()).isEqualTo(AgentConstants.SENSITIVE_ACTION_TAKEOVER);
        assertThat(result.riskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_RED);
    }

    @Test
    void matchUsesRegexAgainstInboundAndReplyContent() {
        AgentSensitiveRuleDO rule = rule("金额正则", AgentConstants.ESCALATION_TRIGGER_REGEX,
                ".*(\\d+\\s*元|\\d+\\s*块).*", AgentConstants.SENSITIVE_ACTION_REVIEW, AgentConstants.RISK_LEVEL_YELLOW);

        AgentSensitiveRuleMatcher.MatchResult result = matcher.match(new AgentRuleMatchContext(
                "费用是多少", "可以给您优惠 200 元", AgentIntentAnalysis.none(),
                AgentConstants.CUSTOMER_LEVEL_NORMAL, List.of(), false), List.of(rule));

        assertThat(result.matched()).isTrue();
        assertThat(result.triggerType()).isEqualTo(AgentConstants.ESCALATION_TRIGGER_REGEX);
    }

    @Test
    void matchUsesCustomerLevel() {
        AgentSensitiveRuleDO rule = rule("重要客户确认", AgentConstants.ESCALATION_TRIGGER_CUSTOMER_LEVEL,
                "IMPORTANT", AgentConstants.SENSITIVE_ACTION_REVIEW, AgentConstants.RISK_LEVEL_YELLOW);

        AgentSensitiveRuleMatcher.MatchResult result = matcher.match(new AgentRuleMatchContext(
                "咨询报价", "", AgentIntentAnalysis.none(),
                AgentConstants.CUSTOMER_LEVEL_IMPORTANT, List.of(), false), List.of(rule));

        assertThat(result.matched()).isTrue();
        assertThat(result.triggerType()).isEqualTo(AgentConstants.ESCALATION_TRIGGER_CUSTOMER_LEVEL);
    }

    @Test
    void matchUsesHumanRequestFromIntentAnalysis() {
        AgentSensitiveRuleDO rule = rule("要求人工", AgentConstants.ESCALATION_TRIGGER_REQUEST_HUMAN,
                "", AgentConstants.SENSITIVE_ACTION_REVIEW, AgentConstants.RISK_LEVEL_YELLOW);
        AgentIntentAnalysis intent = new AgentIntentAnalysis("human_request", "neutral", true,
                AgentConstants.RISK_LEVEL_YELLOW, "0.82", "客户要求人工介入");

        AgentSensitiveRuleMatcher.MatchResult result = matcher.match(new AgentRuleMatchContext(
                "转人工", "", intent, AgentConstants.CUSTOMER_LEVEL_NORMAL, List.of(), false), List.of(rule));

        assertThat(result.matched()).isTrue();
        assertThat(result.ruleName()).isEqualTo("要求人工");
        assertThat(result.triggerType()).isEqualTo(AgentConstants.ESCALATION_TRIGGER_REQUEST_HUMAN);
    }

    private AgentSensitiveRuleDO rule(String name, String triggerType, String pattern, Integer action,
                                      Integer riskLevel) {
        AgentSensitiveRuleDO rule = new AgentSensitiveRuleDO();
        rule.setName(name);
        rule.setTriggerType(triggerType);
        rule.setPattern(pattern);
        rule.setAction(action);
        rule.setRiskLevel(riskLevel);
        rule.setStatus(AgentConstants.STATUS_ENABLE);
        return rule;
    }

}
