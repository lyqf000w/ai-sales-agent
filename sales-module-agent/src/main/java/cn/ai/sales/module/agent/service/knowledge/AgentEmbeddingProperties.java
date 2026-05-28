package cn.ai.sales.module.agent.service.knowledge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent.rag.embedding")
@Data
public class AgentEmbeddingProperties {

    private String url;
    private String model = "qwen3-embedding-4b";
    private String apiKey;
    private Integer topK = 5;
    private Double maxDistance = 0.75;
    private Integer chunkSize = 900;
    private Integer chunkOverlap = 120;

}
