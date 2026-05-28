package cn.ai.sales.module.agent.service.knowledge;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeBasePageReqVO;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeBaseSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeBaseDO;
import jakarta.validation.Valid;

import java.util.List;

public interface AgentKnowledgeBaseService {

    Long createKnowledgeBase(@Valid AgentKnowledgeBaseSaveReqVO createReqVO);

    void updateKnowledgeBase(@Valid AgentKnowledgeBaseSaveReqVO updateReqVO);

    void deleteKnowledgeBase(Long id);

    AgentKnowledgeBaseDO getKnowledgeBase(Long id);

    PageResult<AgentKnowledgeBaseDO> getKnowledgeBasePage(AgentKnowledgeBasePageReqVO pageReqVO);

    List<AgentKnowledgeBaseDO> getEnabledKnowledgeBaseList();

}
