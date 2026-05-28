package cn.ai.sales.module.agent.service.webhook;

import java.util.Map;

public interface AgentWebhookService {

    void handleGeweCallback(String callbackToken, Map<String, Object> payload, String signature);

}
