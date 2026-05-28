package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.framework.tenant.core.context.TenantContextHolder;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentReplyDecisionDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.collection.CollUtil;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentReplyDecisionMapper extends BaseMapperX<AgentReplyDecisionDO> {

    default PageResult<AgentReplyDecisionDO> selectPage(AgentReplyReviewPageReqVO reqVO) {
        LambdaQueryWrapperX<AgentReplyDecisionDO> query = new LambdaQueryWrapperX<AgentReplyDecisionDO>()
                .eqIfPresent(AgentReplyDecisionDO::getConversationId, reqVO.getConversationId())
                .eqIfPresent(AgentReplyDecisionDO::getReviewStatus, reqVO.getReviewStatus())
                .eqIfPresent(AgentReplyDecisionDO::getRiskLevel, reqVO.getRiskLevel());
        if (AgentConstants.REVIEW_STATUS_PENDING.equals(reqVO.getReviewStatus())) {
            filterActionablePendingSuggestions(query);
        }
        return selectPage(reqVO, query.orderByDesc(AgentReplyDecisionDO::getId));
    }

    default AgentReplyDecisionDO selectBySuggestedMessageId(Long suggestedMessageId) {
        return selectOne(AgentReplyDecisionDO::getSuggestedMessageId, suggestedMessageId);
    }

    default Long selectPendingReviewCount() {
        LambdaQueryWrapperX<AgentReplyDecisionDO> query = new LambdaQueryWrapperX<AgentReplyDecisionDO>()
                .eq(AgentReplyDecisionDO::getReviewStatus, AgentConstants.REVIEW_STATUS_PENDING);
        filterActionablePendingSuggestions(query);
        return selectCount(query);
    }

    default List<AgentReplyDecisionDO> selectPendingListByConversationId(Long conversationId) {
        return selectList(new LambdaQueryWrapperX<AgentReplyDecisionDO>()
                .eq(AgentReplyDecisionDO::getConversationId, conversationId)
                .eq(AgentReplyDecisionDO::getReviewStatus, AgentConstants.REVIEW_STATUS_PENDING)
                .orderByDesc(AgentReplyDecisionDO::getId));
    }

    default List<AgentReplyDecisionDO> selectRecentList(int limit) {
        return selectList(new LambdaQueryWrapperX<AgentReplyDecisionDO>()
                .orderByDesc(AgentReplyDecisionDO::getId)
                .last("LIMIT " + Math.max(1, limit)));
    }

    default AgentReplyDecisionDO selectLatest() {
        return CollUtil.getFirst(selectRecentList(1));
    }

    private static void filterActionablePendingSuggestions(LambdaQueryWrapperX<AgentReplyDecisionDO> query) {
        query.isNotNull(AgentReplyDecisionDO::getSuggestedMessageId)
                .inSql(AgentReplyDecisionDO::getSuggestedMessageId,
                        "SELECT id FROM agent_message WHERE deleted = 0 AND tenant_id = "
                                + TenantContextHolder.getRequiredTenantId()
                                + " AND send_status = " + AgentConstants.SEND_STATUS_PENDING_REVIEW);
    }

}
