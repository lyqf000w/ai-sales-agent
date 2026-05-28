package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.controller.admin.sensitiverule.vo.AgentSensitiveRulePageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentSensitiveRuleDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentSensitiveRuleMapper extends BaseMapperX<AgentSensitiveRuleDO> {

    default PageResult<AgentSensitiveRuleDO> selectPage(AgentSensitiveRulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AgentSensitiveRuleDO>()
                .likeIfPresent(AgentSensitiveRuleDO::getName, reqVO.getKeyword())
                .eqIfPresent(AgentSensitiveRuleDO::getMatchType, reqVO.getMatchType())
                .eqIfPresent(AgentSensitiveRuleDO::getTriggerType, reqVO.getTriggerType())
                .eqIfPresent(AgentSensitiveRuleDO::getAction, reqVO.getAction())
                .eqIfPresent(AgentSensitiveRuleDO::getAgentId, reqVO.getAgentId())
                .eqIfPresent(AgentSensitiveRuleDO::getRouteApp, reqVO.getRouteApp())
                .eqIfPresent(AgentSensitiveRuleDO::getStatus, reqVO.getStatus())
                .orderByAsc(AgentSensitiveRuleDO::getSort)
                .orderByDesc(AgentSensitiveRuleDO::getId));
    }

    default List<AgentSensitiveRuleDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<AgentSensitiveRuleDO>()
                .eq(AgentSensitiveRuleDO::getStatus, AgentConstants.STATUS_ENABLE)
                .orderByAsc(AgentSensitiveRuleDO::getSort)
                .orderByDesc(AgentSensitiveRuleDO::getId));
    }

    default AgentSensitiveRuleDO selectByName(String name) {
        return selectOne(AgentSensitiveRuleDO::getName, name);
    }

}
