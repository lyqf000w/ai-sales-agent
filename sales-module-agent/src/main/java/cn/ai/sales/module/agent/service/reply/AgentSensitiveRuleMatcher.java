package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentSensitiveRuleDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class AgentSensitiveRuleMatcher {

    public MatchResult match(String text, List<AgentSensitiveRuleDO> rules) {
        return match(new AgentRuleMatchContext(text, null, AgentIntentAnalysis.none(),
                AgentConstants.CUSTOMER_LEVEL_NORMAL, List.of(), false), rules);
    }

    public MatchResult match(AgentRuleMatchContext context, List<AgentSensitiveRuleDO> rules) {
        AgentRuleMatchContext matchContext = context == null ? new AgentRuleMatchContext("", null, AgentIntentAnalysis.none(),
                AgentConstants.CUSTOMER_LEVEL_NORMAL, List.of(), false) : context;
        if (rules == null || rules.isEmpty()) {
            return MatchResult.none();
        }
        return rules.stream()
                .filter(rule -> rule != null && AgentConstants.STATUS_ENABLE == nullToZero(rule.getStatus()))
                .filter(rule -> matches(matchContext, rule))
                .max(Comparator.comparing((AgentSensitiveRuleDO rule) -> nullToZero(rule.getRiskLevel()))
                        .thenComparing(rule -> nullToZero(rule.getAction()))
                        .thenComparing(rule -> nullToZero(rule.getSort())))
                .map(rule -> new MatchResult(true, rule.getName(), rule.getAction(), rule.getRiskLevel(),
                        resolveTriggerType(rule), rule.getPattern(), buildReason(matchContext, rule),
                        matchContext.intentAnalysis()))
                .orElseGet(MatchResult::none);
    }

    private boolean matches(AgentRuleMatchContext context, AgentSensitiveRuleDO rule) {
        String triggerType = resolveTriggerType(rule);
        if (AgentConstants.ESCALATION_TRIGGER_KEYWORD.equals(triggerType)) {
            return matchesKeyword(context, rule.getPattern());
        }
        if (AgentConstants.ESCALATION_TRIGGER_REGEX.equals(triggerType)) {
            return matchesRegex(context, rule.getPattern());
        }
        if (AgentConstants.ESCALATION_TRIGGER_INTENT.equals(triggerType)) {
            if (StrUtil.isBlank(rule.getPattern())) {
                return false;
            }
            return StrUtil.equalsIgnoreCase(context.intentAnalysis().intent(), rule.getPattern());
        }
        if (AgentConstants.ESCALATION_TRIGGER_SENTIMENT.equals(triggerType)) {
            if (StrUtil.isBlank(rule.getPattern())) {
                return false;
            }
            return StrUtil.equalsIgnoreCase(context.intentAnalysis().sentiment(), rule.getPattern());
        }
        if (AgentConstants.ESCALATION_TRIGGER_REQUEST_HUMAN.equals(triggerType)) {
            return context.intentAnalysis().needsHuman();
        }
        if (AgentConstants.ESCALATION_TRIGGER_CUSTOMER_LEVEL.equals(triggerType)) {
            return matchesCustomerLevel(context.customerLevel(), rule.getPattern());
        }
        if (AgentConstants.ESCALATION_TRIGGER_RAG_MISS.equals(triggerType)) {
            return context.ragMiss();
        }
        return false;
    }

    private String resolveTriggerType(AgentSensitiveRuleDO rule) {
        if (StrUtil.isNotBlank(rule.getTriggerType())) {
            return rule.getTriggerType();
        }
        if (AgentConstants.SENSITIVE_MATCH_REGEX == nullToZero(rule.getMatchType())) {
            return AgentConstants.ESCALATION_TRIGGER_REGEX;
        }
        if (AgentConstants.SENSITIVE_MATCH_LLM_CLASSIFIER == nullToZero(rule.getMatchType())) {
            return AgentConstants.ESCALATION_TRIGGER_INTENT;
        }
        return AgentConstants.ESCALATION_TRIGGER_KEYWORD;
    }

    private String buildReason(AgentRuleMatchContext context, AgentSensitiveRuleDO rule) {
        String triggerType = resolveTriggerType(rule);
        if (AgentConstants.ESCALATION_TRIGGER_INTENT.equals(triggerType)
                || AgentConstants.ESCALATION_TRIGGER_SENTIMENT.equals(triggerType)
                || AgentConstants.ESCALATION_TRIGGER_REQUEST_HUMAN.equals(triggerType)) {
            return context.intentAnalysis().reason();
        }
        return "命中人工升级规则：" + rule.getName();
    }

    private boolean matchesKeyword(AgentRuleMatchContext context, String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return false;
        }
        String targetText = buildTargetText(context);
        if (StrUtil.isBlank(targetText)) {
            return false;
        }
        for (String keyword : splitPattern(pattern)) {
            if (StrUtil.isNotBlank(keyword) && StrUtil.containsIgnoreCase(targetText, keyword.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRegex(AgentRuleMatchContext context, String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return false;
        }
        String targetText = buildTargetText(context);
        if (StrUtil.isBlank(targetText)) {
            return false;
        }
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(targetText)
                    .find();
        } catch (PatternSyntaxException ignored) {
            return false;
        }
    }

    private boolean matchesCustomerLevel(Integer customerLevel, String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return false;
        }
        return StrUtil.equalsIgnoreCase(pattern.trim(), String.valueOf(nullToZero(customerLevel)))
                || StrUtil.equalsIgnoreCase(pattern.trim(), customerLevelCode(nullToZero(customerLevel)));
    }

    private String buildTargetText(AgentRuleMatchContext context) {
        return String.join("\n", StrUtil.blankToDefault(context.inboundContent(), ""),
                StrUtil.blankToDefault(context.replyContent(), ""));
    }

    private List<String> splitPattern(String pattern) {
        return Arrays.stream(pattern.split("[,，;；、\\n]+"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private String customerLevelCode(int customerLevel) {
        if (customerLevel == AgentConstants.CUSTOMER_LEVEL_IMPORTANT) {
            return "IMPORTANT";
        }
        if (customerLevel == AgentConstants.CUSTOMER_LEVEL_TARGET) {
            return "TARGET";
        }
        return "NORMAL";
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    public record MatchResult(boolean matched,
                              String ruleName,
                              Integer action,
                              Integer riskLevel,
                              String triggerType,
                              String triggerValue,
                              String reason,
                              AgentIntentAnalysis intentAnalysis) {

        public MatchResult(boolean matched, String ruleName, Integer action, Integer riskLevel) {
            this(matched, ruleName, action, riskLevel, null, null, null, AgentIntentAnalysis.none());
        }

        public static MatchResult none() {
            return new MatchResult(false, null, null, AgentConstants.RISK_LEVEL_GREEN,
                    null, null, null, AgentIntentAnalysis.none());
        }

    }

}
