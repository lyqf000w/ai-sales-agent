package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeItemDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.knowledge.AgentKnowledgeHit;

import java.util.List;

public record AgentReplyContext(String inboundContent,
                                List<AgentMessageDO> recentMessages,
                                List<AgentKnowledgeHit> knowledgeHits,
                                Long contactId,
                                Integer customerLevel,
                                List<String> customerTagNames,
                                Long agentId,
                                Long knowledgeBaseId,
                                String systemPrompt,
                                String llmProvider,
                                String llmModel) {

    public AgentReplyContext(String inboundContent, List<AgentMessageDO> recentMessages,
                             List<AgentKnowledgeItemDO> knowledgeItems) {
        this(inboundContent, recentMessages, fromItems(knowledgeItems), null,
                AgentConstants.CUSTOMER_LEVEL_NORMAL, List.of(), null, null, null, null, null);
    }

    private static List<AgentKnowledgeHit> fromItems(List<AgentKnowledgeItemDO> knowledgeItems) {
        if (knowledgeItems == null) {
            return List.of();
        }
        return knowledgeItems.stream()
                .map(item -> new AgentKnowledgeHit(item.getKnowledgeBaseId(), item.getId(), null,
                        item.getTitle(), item.getProductName(), item.getCategory(), item.getKeywords(),
                        item.getQuestion(), item.getAnswer(), item.getDistance(), null, item.getSort()))
                .toList();
    }

}
