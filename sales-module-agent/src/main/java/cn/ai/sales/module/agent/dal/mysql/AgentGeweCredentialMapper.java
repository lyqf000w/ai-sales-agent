package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.dal.dataobject.AgentGeweCredentialDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.collection.CollUtil;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentGeweCredentialMapper extends BaseMapperX<AgentGeweCredentialDO> {

    default AgentGeweCredentialDO selectDefaultEnabled() {
        return CollUtil.getFirst(selectList(new LambdaQueryWrapperX<AgentGeweCredentialDO>()
                .eq(AgentGeweCredentialDO::getStatus, AgentConstants.STATUS_ENABLE)
                .orderByAsc(AgentGeweCredentialDO::getId)));
    }

    default AgentGeweCredentialDO selectFirst() {
        return CollUtil.getFirst(selectList(new LambdaQueryWrapperX<AgentGeweCredentialDO>()
                .orderByAsc(AgentGeweCredentialDO::getId)));
    }

    default List<AgentGeweCredentialDO> selectListByOrder() {
        return selectList(new LambdaQueryWrapperX<AgentGeweCredentialDO>()
                .orderByAsc(AgentGeweCredentialDO::getId));
    }

    default List<AgentGeweCredentialDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<AgentGeweCredentialDO>()
                .eq(AgentGeweCredentialDO::getStatus, AgentConstants.STATUS_ENABLE)
                .orderByAsc(AgentGeweCredentialDO::getId));
    }

    default AgentGeweCredentialDO selectByCallbackToken(String callbackToken) {
        return selectOne(AgentGeweCredentialDO::getCallbackToken, callbackToken);
    }

    default Long selectEnabledCount() {
        return selectCount(new LambdaQueryWrapperX<AgentGeweCredentialDO>()
                .eq(AgentGeweCredentialDO::getStatus, AgentConstants.STATUS_ENABLE));
    }

}
