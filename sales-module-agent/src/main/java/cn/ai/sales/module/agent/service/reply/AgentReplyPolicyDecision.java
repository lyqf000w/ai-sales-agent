package cn.ai.sales.module.agent.service.reply;

public record AgentReplyPolicyDecision(boolean stopReply,
                                       boolean autoSend,
                                       String decisionType,
                                       String reviewStatus,
                                       Integer riskLevel,
                                       Integer conversationStatus,
                                       String decisionReason) {
}
