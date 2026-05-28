package cn.ai.sales.module.agent.service.reply;

import java.util.Map;

public record AgentReplyPolicy(String replyMode,
                               Integer quietSeconds,
                               Map<String, Object> businessHours,
                               String source) {

    public static final String SOURCE_ACCOUNT = "ACCOUNT";
    public static final String SOURCE_CONTACT = "CONTACT";

}
