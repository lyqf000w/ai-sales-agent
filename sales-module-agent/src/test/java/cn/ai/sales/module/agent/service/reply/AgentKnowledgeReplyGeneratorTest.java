package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeItemDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.knowledge.AgentKnowledgeHit;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentKnowledgeReplyGeneratorTest {

    private final AgentKnowledgeReplyGenerator generator = new AgentKnowledgeReplyGenerator();

    @Test
    void generateChoosesHighestPriorityEnabledKnowledgeByKeyword() {
        AgentKnowledgeItemDO basic = item("基础价格", "价格,多少钱", "基础版 199 元/月", 1,
                AgentConstants.STATUS_ENABLE);
        AgentKnowledgeItemDO priority = item("企业报价", "报价,费用", "企业版需要结合席位数报价", 10,
                AgentConstants.STATUS_ENABLE);
        AgentKnowledgeItemDO disabled = item("停用报价", "报价", "这条不应该被命中", 100,
                AgentConstants.STATUS_DISABLE);

        AgentGeneratedReply reply = generator.generate("想了解一下报价和费用", List.of(basic, priority, disabled));

        assertThat(reply.hasContent()).isTrue();
        assertThat(reply.content()).isEqualTo("企业版需要结合席位数报价");
        assertThat(reply.matchedKnowledgeTitle()).isEqualTo("企业报价");
        assertThat(reply.generationSource()).isEqualTo(AgentGeneratedReply.SOURCE_KNOWLEDGE);
    }

    @Test
    void generateReturnsBlankWhenNoEnabledKnowledgeMatches() {
        AgentKnowledgeItemDO item = item("售后", "售后", "我们有售后团队", 1, AgentConstants.STATUS_ENABLE);

        AgentGeneratedReply reply = generator.generate("我想问一下部署周期", List.of(item));

        assertThat(reply.hasContent()).isFalse();
        assertThat(reply.content()).isBlank();
        assertThat(reply.generationSource()).isEqualTo(AgentGeneratedReply.SOURCE_NONE);
    }

    @Test
    void generatePolishesKnowledgeFallbackReplyTone() {
        AgentKnowledgeItemDO item = item("退款", "退款", "您好，感谢您的咨询。退款需要先核对订单号。如有其他问题，请随时联系我们。",
                1, AgentConstants.STATUS_ENABLE);

        AgentGeneratedReply reply = generator.generate("我要退款", List.of(item));

        assertThat(reply.content()).isEqualTo("退款需要先核对订单号。");
        assertThat(reply.matchedKnowledgeTitle()).isEqualTo("退款");
        assertThat(reply.generationSource()).isEqualTo(AgentGeneratedReply.SOURCE_KNOWLEDGE);
    }

    @Test
    void generateUsesLlmClientWhenDeepSeekIsConfigured() {
        ReflectionTestUtils.setField(generator, "llmClient", new FakeLlmClient());
        AgentKnowledgeItemDO item = item("企业版", "企业版", "企业版支持私有化部署", 1,
                AgentConstants.STATUS_ENABLE);
        AgentReplyContext context = new AgentReplyContext("企业版怎么部署", List.of(), List.of(hit(item, 80)),
                1L, AgentConstants.CUSTOMER_LEVEL_NORMAL, List.of("高意向"), 1L, 1L,
                "像专业销售一样回答", "DEEPSEEK", "deepseek-v4-pro");

        AgentGeneratedReply reply = generator.generate(context);

        assertThat(reply.content()).isEqualTo("这是 DeepSeek 生成的回复");
        assertThat(reply.matchedKnowledgeTitle()).isEqualTo("企业版");
        assertThat(reply.generationSource()).isEqualTo(AgentGeneratedReply.SOURCE_DEEPSEEK);
        assertThat(reply.llmProvider()).isEqualTo("DEEPSEEK");
        assertThat(reply.llmModel()).isEqualTo("deepseek-v4-pro");
    }

    private AgentKnowledgeItemDO item(String title, String keywords, String answer, Integer sort, Integer status) {
        AgentKnowledgeItemDO item = new AgentKnowledgeItemDO();
        item.setTitle(title);
        item.setKeywords(keywords);
        item.setAnswer(answer);
        item.setSort(sort);
        item.setStatus(status);
        return item;
    }

    private static class FakeLlmClient implements AgentLlmClient {

        @Override
        public boolean supports(String provider, String model) {
            return "DEEPSEEK".equals(provider);
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public AgentGeneratedReply generate(AgentReplyContext context) {
            return AgentGeneratedReply.llm("这是 DeepSeek 生成的回复", context.knowledgeHits().get(0).title(),
                    null, "DEEPSEEK", context.llmModel());
        }

    }

    private AgentKnowledgeHit hit(AgentKnowledgeItemDO item, int keywordScore) {
        return new AgentKnowledgeHit(item.getKnowledgeBaseId(), item.getId(), 1L, item.getTitle(),
                item.getProductName(), item.getCategory(), item.getKeywords(), item.getQuestion(), item.getAnswer(),
                null, keywordScore, item.getSort());
    }

}
