package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.controller.admin.tag.vo.AgentContactTagPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentContactTagDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentContactTagMapper extends BaseMapperX<AgentContactTagDO> {

    default PageResult<AgentContactTagDO> selectPage(AgentContactTagPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AgentContactTagDO>()
                .likeIfPresent(AgentContactTagDO::getName, reqVO.getKeyword())
                .eqIfPresent(AgentContactTagDO::getStatus, reqVO.getStatus())
                .orderByAsc(AgentContactTagDO::getSort)
                .orderByDesc(AgentContactTagDO::getId));
    }

    default List<AgentContactTagDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<AgentContactTagDO>()
                .eq(AgentContactTagDO::getStatus, AgentConstants.STATUS_ENABLE)
                .orderByAsc(AgentContactTagDO::getSort)
                .orderByDesc(AgentContactTagDO::getId));
    }

    default AgentContactTagDO selectByName(String name) {
        return selectOne(AgentContactTagDO::getName, name);
    }

}
