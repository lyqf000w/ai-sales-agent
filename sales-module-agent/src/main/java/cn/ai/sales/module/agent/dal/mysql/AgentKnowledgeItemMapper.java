package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeItemPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeItemDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

import java.util.List;

@Mapper
public interface AgentKnowledgeItemMapper extends BaseMapperX<AgentKnowledgeItemDO> {

    default PageResult<AgentKnowledgeItemDO> selectPage(AgentKnowledgeItemPageReqVO reqVO) {
        LambdaQueryWrapperX<AgentKnowledgeItemDO> query = new LambdaQueryWrapperX<AgentKnowledgeItemDO>()
                .eqIfPresent(AgentKnowledgeItemDO::getKnowledgeBaseId, reqVO.getKnowledgeBaseId())
                .eqIfPresent(AgentKnowledgeItemDO::getStatus, reqVO.getStatus());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            query.and(wrapper -> wrapper
                        .like(AgentKnowledgeItemDO::getTitle, reqVO.getKeyword())
                        .or()
                        .like(AgentKnowledgeItemDO::getProductName, reqVO.getKeyword())
                        .or()
                        .like(AgentKnowledgeItemDO::getKeywords, reqVO.getKeyword())
                        .or()
                        .like(AgentKnowledgeItemDO::getQuestion, reqVO.getKeyword()));
        }
        return selectPage(reqVO, query
                .orderByDesc(AgentKnowledgeItemDO::getSort)
                .orderByDesc(AgentKnowledgeItemDO::getId));
    }

    default List<AgentKnowledgeItemDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<AgentKnowledgeItemDO>()
                .eq(AgentKnowledgeItemDO::getStatus, AgentConstants.STATUS_ENABLE)
                .orderByDesc(AgentKnowledgeItemDO::getSort)
                .orderByDesc(AgentKnowledgeItemDO::getId));
    }

    default List<AgentKnowledgeItemDO> selectEnabledListByKnowledgeBaseId(Long knowledgeBaseId) {
        return selectList(new LambdaQueryWrapperX<AgentKnowledgeItemDO>()
                .eq(AgentKnowledgeItemDO::getKnowledgeBaseId, knowledgeBaseId)
                .eq(AgentKnowledgeItemDO::getStatus, AgentConstants.STATUS_ENABLE)
                .orderByDesc(AgentKnowledgeItemDO::getSort)
                .orderByDesc(AgentKnowledgeItemDO::getId));
    }

    default AgentKnowledgeItemDO selectByTitle(Long knowledgeBaseId, String title) {
        return selectOne(AgentKnowledgeItemDO::getKnowledgeBaseId, knowledgeBaseId,
                AgentKnowledgeItemDO::getTitle, title);
    }

    @Select("""
            SELECT id, knowledge_base_id, title, product_name, category, keywords, question, answer,
                   embedding_status, sort, status,
                   embedding <=> #{queryEmbedding,typeHandler=cn.ai.sales.module.agent.dal.typehandler.PgVectorTypeHandler} AS distance,
                   creator, create_time, updater, update_time, deleted, tenant_id
            FROM agent_knowledge_item
            WHERE deleted = 0
              AND status = 0
              AND knowledge_base_id = #{knowledgeBaseId}
              AND embedding IS NOT NULL
            ORDER BY embedding <=> #{queryEmbedding,typeHandler=cn.ai.sales.module.agent.dal.typehandler.PgVectorTypeHandler}
            LIMIT #{limit}
            """)
    List<AgentKnowledgeItemDO> selectVectorMatches(@Param("knowledgeBaseId") Long knowledgeBaseId,
                                                   @Param("queryEmbedding") List<Double> queryEmbedding,
                                                   @Param("limit") Integer limit);

}
