package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeBasePageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeBaseDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentKnowledgeBaseMapper extends BaseMapperX<AgentKnowledgeBaseDO> {

    default PageResult<AgentKnowledgeBaseDO> selectPage(AgentKnowledgeBasePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AgentKnowledgeBaseDO>()
                .likeIfPresent(AgentKnowledgeBaseDO::getName, reqVO.getKeyword())
                .eqIfPresent(AgentKnowledgeBaseDO::getStatus, reqVO.getStatus())
                .orderByDesc(AgentKnowledgeBaseDO::getId));
    }

    default List<AgentKnowledgeBaseDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<AgentKnowledgeBaseDO>()
                .eq(AgentKnowledgeBaseDO::getStatus, AgentConstants.STATUS_ENABLE)
                .orderByDesc(AgentKnowledgeBaseDO::getId));
    }

    default AgentKnowledgeBaseDO selectByName(String name) {
        return selectOne(AgentKnowledgeBaseDO::getName, name);
    }

}
