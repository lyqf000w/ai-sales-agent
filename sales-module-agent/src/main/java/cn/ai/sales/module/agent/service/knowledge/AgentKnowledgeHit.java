package cn.ai.sales.module.agent.service.knowledge;

import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeChunkDO;
import cn.hutool.core.util.StrUtil;

public record AgentKnowledgeHit(Long knowledgeBaseId,
                                Long knowledgeItemId,
                                Long knowledgeChunkId,
                                String title,
                                String productName,
                                String category,
                                String keywords,
                                String question,
                                String content,
                                Double distance,
                                Integer keywordScore,
                                Integer sort) {

    public static AgentKnowledgeHit fromChunk(AgentKnowledgeChunkDO chunk, Integer keywordScore) {
        return new AgentKnowledgeHit(chunk.getKnowledgeBaseId(), chunk.getKnowledgeItemId(), chunk.getId(),
                chunk.getTitle(), chunk.getProductName(), chunk.getCategory(), chunk.getKeywords(),
                chunk.getQuestion(), chunk.getContent(), chunk.getDistance(), keywordScore, chunk.getSort());
    }

    public boolean hasContent() {
        return StrUtil.isNotBlank(content);
    }

}
