package cn.ai.sales.module.agent.service.knowledge;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeItemPageReqVO;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeItemSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeItemDO;
import jakarta.validation.Valid;

public interface AgentKnowledgeItemService {

    Long createKnowledgeItem(@Valid AgentKnowledgeItemSaveReqVO createReqVO);

    void updateKnowledgeItem(@Valid AgentKnowledgeItemSaveReqVO updateReqVO);

    void deleteKnowledgeItem(Long id);

    AgentKnowledgeItemDO getKnowledgeItem(Long id);

    PageResult<AgentKnowledgeItemDO> getKnowledgeItemPage(AgentKnowledgeItemPageReqVO pageReqVO);

    void rebuildKnowledgeItemIndex(Long id);

}
