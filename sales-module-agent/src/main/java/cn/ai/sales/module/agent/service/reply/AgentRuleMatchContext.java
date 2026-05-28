package cn.ai.sales.module.agent.service.reply;

import java.util.List;

public record AgentRuleMatchContext(String inboundContent,
                                    String replyContent,
                                    AgentIntentAnalysis intentAnalysis,
                                    Integer customerLevel,
                                    List<String> customerTagNames,
                                    boolean ragMiss) {

    public AgentRuleMatchContext {
        intentAnalysis = intentAnalysis == null ? AgentIntentAnalysis.none() : intentAnalysis;
        customerTagNames = customerTagNames == null ? List.of() : customerTagNames;
    }

}
