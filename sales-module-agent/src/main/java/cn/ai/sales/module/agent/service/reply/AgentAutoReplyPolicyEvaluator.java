package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;

@Component
public class AgentAutoReplyPolicyEvaluator {

    public AgentReplyPolicyDecision evaluate(AgentReplyPolicy policy, AgentConversationDO conversation,
                                             AgentGeneratedReply generatedReply,
                                             AgentSensitiveRuleMatcher.MatchResult sensitiveMatch,
                                             boolean canSend, LocalDateTime now) {
        return evaluate(policy, conversation, generatedReply, sensitiveMatch, canSend, now, false);
    }

    public AgentReplyPolicyDecision evaluate(AgentReplyPolicy policy, AgentConversationDO conversation,
                                             AgentGeneratedReply generatedReply,
                                             AgentSensitiveRuleMatcher.MatchResult sensitiveMatch,
                                             boolean canSend, LocalDateTime now, boolean ignoreQuietWindow) {
        if (Objects.equals(conversation.getStatus(), AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER)
                && !AgentConstants.REPLY_MODE_AUTO_REPLY.equals(policy.replyMode())) {
            return stop("会话已人工接管，暂停自动回复", AgentConstants.RISK_LEVEL_RED,
                    AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
        }
        if (sensitiveMatch.matched()
                && !Objects.equals(sensitiveMatch.action(), AgentConstants.SENSITIVE_ACTION_ALLOW)) {
            if (Objects.equals(sensitiveMatch.action(), AgentConstants.SENSITIVE_ACTION_TAKEOVER)) {
                return stop("命中人工升级规则，转人工接管", sensitiveMatch.riskLevel(),
                        AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
            }
            if (Objects.equals(sensitiveMatch.action(), AgentConstants.SENSITIVE_ACTION_BLOCK)) {
                return stop("命中阻断规则，暂停自动回复", sensitiveMatch.riskLevel(),
                        AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
            }
            return manual("命中人工升级规则，需人工确认", sensitiveMatch.riskLevel(),
                    AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM, AgentConstants.DECISION_TYPE_MANUAL_CONFIRM);
        }
        if (AgentConstants.REPLY_MODE_RECORD_ONLY.equals(policy.replyMode())) {
            return stop("策略为仅记录模式", AgentConstants.RISK_LEVEL_GREEN,
                    AgentConstants.CONVERSATION_STATUS_OPEN);
        }
        if (AgentConstants.REPLY_MODE_MANUAL_CONFIRM.equals(policy.replyMode())) {
            return manual("策略为人工确认模式", AgentConstants.RISK_LEVEL_GREEN,
                    AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM, AgentConstants.DECISION_TYPE_MANUAL_CONFIRM);
        }
        if (!AgentConstants.REPLY_MODE_AUTO_REPLY.equals(policy.replyMode())) {
            return manual("策略未启用自动回复", AgentConstants.RISK_LEVEL_GREEN,
                    AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM, AgentConstants.DECISION_TYPE_MANUAL_CONFIRM);
        }
        if (!canSend) {
            return manual("Gewe 发送配置缺失，需人工发送或补充配置", AgentConstants.RISK_LEVEL_GREEN,
                    AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM, AgentConstants.DECISION_TYPE_MANUAL_CONFIRM);
        }
        if (!isWithinBusinessHours(policy.businessHours(), now.toLocalTime())) {
            return manual("当前不在策略营业时间内，需人工确认", AgentConstants.RISK_LEVEL_YELLOW,
                    AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM, AgentConstants.DECISION_TYPE_MANUAL_CONFIRM);
        }
        if (!ignoreQuietWindow && isWithinQuietMinutes(policy, conversation, now)) {
            return manual("当前仍在静默时间内，需人工确认", AgentConstants.RISK_LEVEL_YELLOW,
                    AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM, AgentConstants.DECISION_TYPE_MANUAL_CONFIRM);
        }
        return new AgentReplyPolicyDecision(false, true, AgentConstants.DECISION_TYPE_AUTO_SEND,
                AgentConstants.REVIEW_STATUS_SENT, AgentConstants.RISK_LEVEL_GREEN,
                AgentConstants.CONVERSATION_STATUS_AI_AUTO, "满足自动回复条件");
    }

    private AgentReplyPolicyDecision stop(String reason, Integer riskLevel, Integer conversationStatus) {
        return new AgentReplyPolicyDecision(true, false, AgentConstants.DECISION_TYPE_HUMAN_TAKEOVER,
                AgentConstants.REVIEW_STATUS_PENDING, riskLevel, conversationStatus, reason);
    }

    private AgentReplyPolicyDecision manual(String reason, Integer riskLevel, Integer conversationStatus,
                                            String decisionType) {
        return new AgentReplyPolicyDecision(false, false, decisionType, AgentConstants.REVIEW_STATUS_PENDING,
                riskLevel, conversationStatus, reason);
    }

    private boolean isWithinBusinessHours(Map<String, Object> businessHours, LocalTime now) {
        if (businessHours == null || businessHours.isEmpty()) {
            return true;
        }
        String startText = Objects.toString(businessHours.get("start"), "");
        String endText = Objects.toString(businessHours.get("end"), "");
        if (StrUtil.isBlank(startText) || StrUtil.isBlank(endText)) {
            return true;
        }
        try {
            LocalTime start = LocalTime.parse(startText);
            LocalTime end = LocalTime.parse(endText);
            if (start.equals(end)) {
                return true;
            }
            if (start.isBefore(end)) {
                return !now.isBefore(start) && !now.isAfter(end);
            }
            return !now.isBefore(start) || !now.isAfter(end);
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private boolean isWithinQuietMinutes(AgentReplyPolicy policy, AgentConversationDO conversation, LocalDateTime now) {
        Integer quietSeconds = policy.quietSeconds();
        if (quietSeconds == null || quietSeconds <= 0 || conversation.getLastMessageTime() == null) {
            return false;
        }
        return conversation.getLastMessageTime().plusSeconds(quietSeconds).isAfter(now);
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

}
