package cn.ai.sales.module.agent.service.knowledge;

import java.util.List;

public interface AgentKnowledgeRetrievalService {

    List<AgentKnowledgeHit> retrieve(Long knowledgeBaseId, String query);

}
