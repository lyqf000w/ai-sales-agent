package cn.ai.sales.module.agent.service.knowledge;

import java.util.List;

public interface AgentEmbeddingClient {

    boolean isEnabled();

    List<Double> embed(String text);

}
