package cn.ai.sales.module.agent.service.knowledge;

import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeItemDO;

public interface AgentKnowledgeIndexService {

    void rebuildItemIndex(AgentKnowledgeItemDO item);

    void deleteItemIndex(Long knowledgeItemId);

}
