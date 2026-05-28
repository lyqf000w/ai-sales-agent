package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.dal.dataobject.AgentConfigVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentConfigVersionMapper extends BaseMapperX<AgentConfigVersionDO> {

    default List<AgentConfigVersionDO> selectListByAgentId(Long agentId) {
        return selectList(new LambdaQueryWrapperX<AgentConfigVersionDO>()
                .eq(AgentConfigVersionDO::getAgentId, agentId)
                .orderByDesc(AgentConfigVersionDO::getVersion)
                .orderByDesc(AgentConfigVersionDO::getId));
    }

    default AgentConfigVersionDO selectByAgentIdAndVersion(Long agentId, Integer version) {
        return selectOne(AgentConfigVersionDO::getAgentId, agentId,
                AgentConfigVersionDO::getVersion, version);
    }

}
