package cn.ai.sales.module.agent.service.reply;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent.llm.deepseek")
@Data
public class AgentDeepSeekLlmProperties {

    private Boolean enabled = true;
    private String url = "https://api.deepseek.com/chat/completions";
    private String apiKey;
    private String model = "deepseek-v4-pro";
    private Double temperature = 0.35;
    private Integer maxTokens = 800;
    private Boolean thinkingEnabled = false;

}
