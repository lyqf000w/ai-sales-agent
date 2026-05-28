package cn.ai.sales.module.agent.service.sensitiverule;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.sensitiverule.vo.AgentSensitiveRuleOptionsRespVO;
import cn.ai.sales.module.agent.controller.admin.sensitiverule.vo.AgentSensitiveRulePageReqVO;
import cn.ai.sales.module.agent.controller.admin.sensitiverule.vo.AgentSensitiveRuleSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentSensitiveRuleDO;
import jakarta.validation.Valid;

public interface AgentSensitiveRuleService {

    Long createRule(@Valid AgentSensitiveRuleSaveReqVO createReqVO);

    void updateRule(@Valid AgentSensitiveRuleSaveReqVO updateReqVO);

    void deleteRule(Long id);

    AgentSensitiveRuleDO getRule(Long id);

    PageResult<AgentSensitiveRuleDO> getRulePage(AgentSensitiveRulePageReqVO pageReqVO);

    AgentSensitiveRuleOptionsRespVO getRuleOptions();

}
