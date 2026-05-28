package cn.ai.sales.module.agent.service.knowledge;

import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeChunkDO;
import cn.ai.sales.module.agent.dal.mysql.AgentKnowledgeChunkMapper;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentKnowledgeRetrievalServiceImpl implements AgentKnowledgeRetrievalService {

    @Resource
    private AgentKnowledgeChunkMapper knowledgeChunkMapper;
    @Resource
    private AgentEmbeddingClient embeddingClient;
    @Resource
    private AgentEmbeddingProperties embeddingProperties;

    @Override
    public List<AgentKnowledgeHit> retrieve(Long knowledgeBaseId, String query) {
        if (knowledgeBaseId == null || StrUtil.isBlank(query)) {
            return List.of();
        }
        Map<Long, AgentKnowledgeHit> candidates = new LinkedHashMap<>();
        List<Double> queryEmbedding = embeddingClient.embed(query);
        if (!queryEmbedding.isEmpty()) {
            List<AgentKnowledgeChunkDO> vectorMatches = knowledgeChunkMapper.selectVectorMatches(knowledgeBaseId,
                    queryEmbedding, topK() * 2);
            for (AgentKnowledgeChunkDO chunk : vectorMatches) {
                if (chunk.getDistance() == null || chunk.getDistance() <= maxDistance()) {
                    candidates.put(chunk.getId(), AgentKnowledgeHit.fromChunk(chunk, keywordScore(query, chunk)));
                }
            }
        }
        for (AgentKnowledgeChunkDO chunk : knowledgeChunkMapper.selectEnabledListByKnowledgeBaseId(knowledgeBaseId)) {
            int score = keywordScore(query, chunk);
            if (score > 0) {
                candidates.putIfAbsent(chunk.getId(), AgentKnowledgeHit.fromChunk(chunk, score));
            }
        }
        return candidates.values().stream()
                .sorted(Comparator
                        .comparing((AgentKnowledgeHit hit) -> hit.distance() == null ? 1D : hit.distance())
                        .thenComparing(hit -> hit.keywordScore() == null ? 0 : hit.keywordScore(), Comparator.reverseOrder())
                        .thenComparing(hit -> hit.sort() == null ? 0 : hit.sort(), Comparator.reverseOrder())
                        .thenComparing(hit -> hit.knowledgeChunkId() == null ? 0L : hit.knowledgeChunkId(),
                                Comparator.reverseOrder()))
                .limit(topK())
                .toList();
    }

    private int keywordScore(String query, AgentKnowledgeChunkDO chunk) {
        int score = 0;
        if (contains(query, chunk.getProductName())) {
            score += 30;
        }
        if (contains(query, chunk.getCategory())) {
            score += 10;
        }
        if (contains(query, chunk.getTitle())) {
            score += 15;
        }
        if (StrUtil.isNotBlank(chunk.getKeywords())) {
            for (String keyword : chunk.getKeywords().split("[,，;；\\s]+")) {
                if (contains(query, keyword)) {
                    score += 20;
                }
            }
        }
        if (contains(query, chunk.getQuestion())) {
            score += 40;
        }
        if (contains(chunk.getContent(), query) || contains(query, chunk.getContent())) {
            score += 5;
        }
        return score;
    }

    private boolean contains(String source, String value) {
        return StrUtil.isNotBlank(source) && StrUtil.isNotBlank(value)
                && StrUtil.containsIgnoreCase(source, value.trim());
    }

    private int topK() {
        return embeddingProperties.getTopK() == null || embeddingProperties.getTopK() <= 0
                ? 5 : embeddingProperties.getTopK();
    }

    private double maxDistance() {
        return embeddingProperties.getMaxDistance() == null ? 0.75 : embeddingProperties.getMaxDistance();
    }
}
