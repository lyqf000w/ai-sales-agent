package cn.ai.sales.module.agent.service.knowledge;

import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeItemDO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentKnowledgeChunkerTest {

    private final AgentKnowledgeChunker chunker = new AgentKnowledgeChunker();

    @Test
    void chunkKeepsShortKnowledgeAsOneSearchableSnippet() {
        AgentKnowledgeItemDO item = item("企业版报价", "报价,费用", "企业版需要结合坐席数和部署方式报价。");

        List<String> chunks = chunker.chunk(item, 900, 120);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0))
                .contains("标题：企业版报价")
                .contains("关键词：报价,费用")
                .contains("参考答案：企业版需要结合坐席数和部署方式报价。");
    }

    @Test
    void chunkSplitsLongKnowledgeWithOverlap() {
        AgentKnowledgeItemDO item = item("部署说明", "部署", "A".repeat(1200));

        List<String> chunks = chunker.chunk(item, 500, 80);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allMatch(chunk -> chunk.length() <= 500);
        assertThat(chunks.get(0)).endsWith("A");
        assertThat(chunks.get(1)).startsWith("A");
    }

    private AgentKnowledgeItemDO item(String title, String keywords, String answer) {
        AgentKnowledgeItemDO item = new AgentKnowledgeItemDO();
        item.setTitle(title);
        item.setProductName("AI 销冠");
        item.setCategory("销售资料");
        item.setKeywords(keywords);
        item.setQuestion("客户问这个资料怎么用");
        item.setAnswer(answer);
        return item;
    }

}
