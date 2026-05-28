package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentMapper extends BaseMapperX<AgentDO> {

    default PageResult<AgentDO> selectPage(AgentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AgentDO>()
                .likeIfPresent(AgentDO::getName, reqVO.getKeyword())
                .eqIfPresent(AgentDO::getReplyMode, reqVO.getReplyMode())
                .eqIfPresent(AgentDO::getStatus, reqVO.getStatus())
                .orderByDesc(AgentDO::getId));
    }

    default AgentDO selectByName(String name) {
        return selectOne(AgentDO::getName, name);
    }

    default List<AgentDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<AgentDO>()
                .eq(AgentDO::getStatus, AgentConstants.STATUS_ENABLE)
                .orderByDesc(AgentDO::getId));
    }

}
