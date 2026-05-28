package cn.ai.sales.module.agent.service.agent;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentLlmModelOptionRespVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentPageReqVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentPublishReqVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConfigVersionDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentDO;
import jakarta.validation.Valid;

import java.util.List;

public interface AgentService {

    Long createAgent(@Valid AgentSaveReqVO createReqVO);

    void updateAgent(@Valid AgentSaveReqVO updateReqVO);

    void publishAgent(@Valid AgentPublishReqVO publishReqVO);

    void deleteAgent(Long id);

    AgentDO getAgent(Long id);

    PageResult<AgentDO> getAgentPage(AgentPageReqVO pageReqVO);

    List<AgentDO> getEnabledAgents();

    List<AgentLlmModelOptionRespVO> getLlmModelOptions();

    List<AgentConfigVersionDO> getConfigVersions(Long agentId);

}
