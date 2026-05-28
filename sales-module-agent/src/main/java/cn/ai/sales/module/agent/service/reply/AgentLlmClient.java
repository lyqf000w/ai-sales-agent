package cn.ai.sales.module.agent.service.reply;

public interface AgentLlmClient {

    boolean supports(String provider, String model);

    boolean isEnabled();

    AgentGeneratedReply generate(AgentReplyContext context);

}
