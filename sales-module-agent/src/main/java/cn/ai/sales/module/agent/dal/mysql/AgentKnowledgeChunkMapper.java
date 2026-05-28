package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeChunkDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentKnowledgeChunkMapper extends BaseMapperX<AgentKnowledgeChunkDO> {

    default void deleteByKnowledgeItemId(Long knowledgeItemId) {
        delete(new LambdaQueryWrapperX<AgentKnowledgeChunkDO>()
                .eq(AgentKnowledgeChunkDO::getKnowledgeItemId, knowledgeItemId));
    }

    default List<AgentKnowledgeChunkDO> selectEnabledListByKnowledgeBaseId(Long knowledgeBaseId) {
        return selectList(new LambdaQueryWrapperX<AgentKnowledgeChunkDO>()
                .eq(AgentKnowledgeChunkDO::getKnowledgeBaseId, knowledgeBaseId)
                .eq(AgentKnowledgeChunkDO::getStatus, AgentConstants.STATUS_ENABLE)
                .orderByDesc(AgentKnowledgeChunkDO::getSort)
                .orderByAsc(AgentKnowledgeChunkDO::getChunkNo)
                .orderByDesc(AgentKnowledgeChunkDO::getId));
    }

    @Select("""
            SELECT id, knowledge_base_id, knowledge_item_id, chunk_no, title, product_name, category,
                   keywords, question, content, embedding_status, sort, status,
                   embedding <=> #{queryEmbedding,typeHandler=cn.ai.sales.module.agent.dal.typehandler.PgVectorTypeHandler} AS distance,
                   creator, create_time, updater, update_time, deleted, tenant_id
            FROM agent_knowledge_chunk
            WHERE deleted = 0
              AND status = 0
              AND knowledge_base_id = #{knowledgeBaseId}
              AND embedding IS NOT NULL
            ORDER BY embedding <=> #{queryEmbedding,typeHandler=cn.ai.sales.module.agent.dal.typehandler.PgVectorTypeHandler}
            LIMIT #{limit}
            """)
    List<AgentKnowledgeChunkDO> selectVectorMatches(@Param("knowledgeBaseId") Long knowledgeBaseId,
                                                    @Param("queryEmbedding") List<Double> queryEmbedding,
                                                    @Param("limit") Integer limit);

}
