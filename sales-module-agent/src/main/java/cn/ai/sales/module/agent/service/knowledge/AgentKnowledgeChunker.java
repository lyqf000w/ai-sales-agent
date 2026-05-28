package cn.ai.sales.module.agent.service.knowledge;

import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeItemDO;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AgentKnowledgeChunker {

    public List<String> chunk(AgentKnowledgeItemDO item, int chunkSize, int chunkOverlap) {
        if (item == null || StrUtil.isBlank(item.getAnswer())) {
            return List.of();
        }
        int normalizedChunkSize = Math.max(chunkSize, 300);
        int normalizedOverlap = Math.max(0, Math.min(chunkOverlap, normalizedChunkSize / 3));
        String source = buildSourceText(item);
        if (source.length() <= normalizedChunkSize) {
            return List.of(source);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < source.length()) {
            int end = Math.min(source.length(), start + normalizedChunkSize);
            chunks.add(source.substring(start, end).trim());
            if (end >= source.length()) {
                break;
            }
            start = Math.max(end - normalizedOverlap, start + 1);
        }
        return chunks.stream().filter(StrUtil::isNotBlank).toList();
    }

    private String buildSourceText(AgentKnowledgeItemDO item) {
        List<String> parts = new ArrayList<>();
        if (StrUtil.isNotBlank(item.getTitle())) {
            parts.add("标题：" + item.getTitle());
        }
        if (StrUtil.isNotBlank(item.getProductName())) {
            parts.add("产品：" + item.getProductName());
        }
        if (StrUtil.isNotBlank(item.getCategory())) {
            parts.add("分类：" + item.getCategory());
        }
        if (StrUtil.isNotBlank(item.getKeywords())) {
            parts.add("关键词：" + item.getKeywords());
        }
        if (StrUtil.isNotBlank(item.getQuestion())) {
            parts.add("客户问题：" + item.getQuestion());
        }
        parts.add("参考答案：" + item.getAnswer());
        return String.join("\n", parts);
    }

}
