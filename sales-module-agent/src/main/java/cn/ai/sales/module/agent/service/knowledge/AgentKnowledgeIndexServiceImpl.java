package cn.ai.sales.module.agent.service.knowledge;

import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeChunkDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeItemDO;
import cn.ai.sales.module.agent.dal.mysql.AgentKnowledgeChunkMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentKnowledgeItemMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentKnowledgeIndexServiceImpl implements AgentKnowledgeIndexService {

    @Resource
    private AgentKnowledgeChunkMapper knowledgeChunkMapper;
    @Resource
    private AgentKnowledgeItemMapper knowledgeItemMapper;
    @Resource
    private AgentKnowledgeChunker knowledgeChunker;
    @Resource
    private AgentEmbeddingClient embeddingClient;
    @Resource
    private AgentEmbeddingProperties embeddingProperties;

    @Override
    public void rebuildItemIndex(AgentKnowledgeItemDO item) {
        if (item == null || item.getId() == null) {
            return;
        }
        knowledgeChunkMapper.deleteByKnowledgeItemId(item.getId());
        List<String> chunks = knowledgeChunker.chunk(item, chunkSize(), chunkOverlap());
        if (chunks.isEmpty()) {
            updateItemEmbeddingStatus(item.getId(), null, AgentConstants.EMBEDDING_STATUS_FAILED);
            return;
        }
        List<Double> firstReadyEmbedding = null;
        int readyCount = 0;
        for (int i = 0; i < chunks.size(); i++) {
            AgentKnowledgeChunkDO chunk = buildChunk(item, i + 1, chunks.get(i));
            List<Double> embedding = embeddingClient.embed(buildEmbeddingText(item, chunks.get(i)));
            if (!embedding.isEmpty()) {
                chunk.setEmbedding(embedding);
                chunk.setEmbeddingStatus(AgentConstants.EMBEDDING_STATUS_READY);
                if (firstReadyEmbedding == null) {
                    firstReadyEmbedding = embedding;
                }
                readyCount++;
            } else {
                chunk.setEmbedding(null);
                chunk.setEmbeddingStatus(AgentConstants.EMBEDDING_STATUS_FAILED);
            }
            knowledgeChunkMapper.insert(chunk);
        }
        updateItemEmbeddingStatus(item.getId(), firstReadyEmbedding,
                readyCount > 0 ? AgentConstants.EMBEDDING_STATUS_READY : AgentConstants.EMBEDDING_STATUS_FAILED);
    }

    @Override
    public void deleteItemIndex(Long knowledgeItemId) {
        if (knowledgeItemId != null) {
            knowledgeChunkMapper.deleteByKnowledgeItemId(knowledgeItemId);
        }
    }

    private AgentKnowledgeChunkDO buildChunk(AgentKnowledgeItemDO item, int chunkNo, String content) {
        AgentKnowledgeChunkDO chunk = new AgentKnowledgeChunkDO();
        chunk.setKnowledgeBaseId(item.getKnowledgeBaseId());
        chunk.setKnowledgeItemId(item.getId());
        chunk.setChunkNo(chunkNo);
        chunk.setTitle(item.getTitle());
        chunk.setProductName(item.getProductName());
        chunk.setCategory(item.getCategory());
        chunk.setKeywords(item.getKeywords());
        chunk.setQuestion(item.getQuestion());
        chunk.setContent(content);
        chunk.setSort(item.getSort());
        chunk.setStatus(item.getStatus());
        return chunk;
    }

    private void updateItemEmbeddingStatus(Long itemId, List<Double> embedding, String status) {
        AgentKnowledgeItemDO update = new AgentKnowledgeItemDO();
        update.setId(itemId);
        update.setEmbedding(embedding);
        update.setEmbeddingStatus(status);
        knowledgeItemMapper.updateById(update);
    }

    private String buildEmbeddingText(AgentKnowledgeItemDO item, String chunkContent) {
        return String.join("\n", List.of(
                StrUtil.blankToDefault(item.getProductName(), ""),
                StrUtil.blankToDefault(item.getCategory(), ""),
                StrUtil.blankToDefault(item.getTitle(), ""),
                StrUtil.blankToDefault(item.getKeywords(), ""),
                StrUtil.blankToDefault(item.getQuestion(), ""),
                StrUtil.blankToDefault(chunkContent, "")
        ));
    }

    private int chunkSize() {
        return embeddingProperties.getChunkSize() == null || embeddingProperties.getChunkSize() <= 0
                ? 900 : embeddingProperties.getChunkSize();
    }

    private int chunkOverlap() {
        return embeddingProperties.getChunkOverlap() == null || embeddingProperties.getChunkOverlap() < 0
                ? 120 : embeddingProperties.getChunkOverlap();
    }

}
