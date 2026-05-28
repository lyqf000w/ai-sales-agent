package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationPageReqVO;
import cn.ai.sales.module.agent.controller.admin.risk.vo.AgentRiskPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.framework.tenant.core.context.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AgentConversationMapper extends BaseMapperX<AgentConversationDO> {

    default PageResult<AgentConversationDO> selectPage(AgentConversationPageReqVO reqVO) {
        LambdaQueryWrapperX<AgentConversationDO> query = new LambdaQueryWrapperX<AgentConversationDO>()
                .eqIfPresent(AgentConversationDO::getWechatAccountId, reqVO.getWechatAccountId())
                .eqIfPresent(AgentConversationDO::getContactId, reqVO.getContactId())
                .eqIfPresent(AgentConversationDO::getRiskLevel, reqVO.getRiskLevel());
        if (AgentConstants.CONVERSATION_QUEUE_PENDING_REVIEW.equals(reqVO.getQueueType())) {
            query.eq(AgentConversationDO::getStatus, AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
        } else if (AgentConstants.CONVERSATION_QUEUE_TAKEOVER.equals(reqVO.getQueueType())) {
            query.eq(AgentConversationDO::getStatus, AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
        } else if (AgentConstants.CONVERSATION_QUEUE_FOCUS.equals(reqVO.getQueueType())) {
            filterByContactFollowUpPriority(query, AgentConstants.FOLLOW_UP_PRIORITY_FOCUS);
        } else if (AgentConstants.CONVERSATION_QUEUE_URGENT.equals(reqVO.getQueueType())) {
            filterByContactFollowUpPriority(query, AgentConstants.FOLLOW_UP_PRIORITY_URGENT);
        } else {
            query.eqIfPresent(AgentConversationDO::getStatus, reqVO.getStatus());
            if (AgentConstants.CONVERSATION_QUEUE_RISK.equals(reqVO.getQueueType())) {
                query.gt(AgentConversationDO::getRiskLevel, AgentConstants.RISK_LEVEL_GREEN);
            }
        }
        return selectPage(reqVO, query.orderByDesc(AgentConversationDO::getLastMessageTime));
    }

    private static void filterByContactFollowUpPriority(LambdaQueryWrapperX<AgentConversationDO> query,
                                                        String followUpPriority) {
        query.inSql(AgentConversationDO::getContactId,
                "SELECT id FROM agent_wechat_contact WHERE deleted = 0 AND tenant_id = "
                        + TenantContextHolder.getRequiredTenantId()
                        + " AND follow_up_priority = '" + followUpPriority + "'");
    }

    default PageResult<AgentConversationDO> selectRiskPage(AgentRiskPageReqVO reqVO) {
        LambdaQueryWrapperX<AgentConversationDO> query = new LambdaQueryWrapperX<AgentConversationDO>()
                .eqIfPresent(AgentConversationDO::getWechatAccountId, reqVO.getWechatAccountId())
                .eqIfPresent(AgentConversationDO::getContactId, reqVO.getContactId())
                .eqIfPresent(AgentConversationDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AgentConversationDO::getRiskLevel, reqVO.getRiskLevel());
        if (reqVO.getStatus() == null && reqVO.getRiskLevel() == null) {
            query.gt(AgentConversationDO::getRiskLevel, AgentConstants.RISK_LEVEL_GREEN);
        }
        return selectPage(reqVO, query.orderByDesc(AgentConversationDO::getLastMessageTime));
    }

    default AgentConversationDO selectByAccountAndContact(Long wechatAccountId, Long contactId) {
        return selectOne(AgentConversationDO::getWechatAccountId, wechatAccountId,
                AgentConversationDO::getContactId, contactId);
    }

    default List<AgentConversationDO> selectListByWechatAccountId(Long wechatAccountId) {
        return selectList(AgentConversationDO::getWechatAccountId, wechatAccountId);
    }

    default List<AgentConversationDO> selectDuePendingReplyList(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapperX<AgentConversationDO>()
                .isNotNull(AgentConversationDO::getPendingReplyMessageId)
                .isNotNull(AgentConversationDO::getPendingReplyDueTime)
                .le(AgentConversationDO::getPendingReplyDueTime, now)
                .notIn(AgentConversationDO::getStatus, AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM,
                        AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER)
                .orderByAsc(AgentConversationDO::getPendingReplyDueTime)
                .last("LIMIT " + Math.max(1, limit)));
    }

    default void clearPendingReply(Long conversationId) {
        update(null, new LambdaUpdateWrapper<AgentConversationDO>()
                .eq(AgentConversationDO::getId, conversationId)
                .set(AgentConversationDO::getPendingReplyMessageId, null)
                .set(AgentConversationDO::getPendingReplyDueTime, null));
    }

    default void clearTemporaryTakeover(Long conversationId) {
        update(null, new LambdaUpdateWrapper<AgentConversationDO>()
                .eq(AgentConversationDO::getId, conversationId)
                .set(AgentConversationDO::getHumanTakeoverUserId, null)
                .set(AgentConversationDO::getHumanTakeoverTime, null)
                .set(AgentConversationDO::getPendingReplyMessageId, null)
                .set(AgentConversationDO::getPendingReplyDueTime, null));
    }

    default void resetForAutoReply(Long conversationId) {
        update(null, new LambdaUpdateWrapper<AgentConversationDO>()
                .eq(AgentConversationDO::getId, conversationId)
                .set(AgentConversationDO::getStatus, AgentConstants.CONVERSATION_STATUS_OPEN)
                .set(AgentConversationDO::getRiskLevel, AgentConstants.RISK_LEVEL_GREEN)
                .set(AgentConversationDO::getContinuousAutoReplyCount, 0)
                .set(AgentConversationDO::getHumanTakeoverUserId, null)
                .set(AgentConversationDO::getHumanTakeoverTime, null)
                .set(AgentConversationDO::getPendingReplyMessageId, null)
                .set(AgentConversationDO::getPendingReplyDueTime, null));
    }

    default void resetForAutoReplyByWechatAccountId(Long wechatAccountId) {
        update(null, new LambdaUpdateWrapper<AgentConversationDO>()
                .eq(AgentConversationDO::getWechatAccountId, wechatAccountId)
                .set(AgentConversationDO::getStatus, AgentConstants.CONVERSATION_STATUS_OPEN)
                .set(AgentConversationDO::getRiskLevel, AgentConstants.RISK_LEVEL_GREEN)
                .set(AgentConversationDO::getContinuousAutoReplyCount, 0)
                .set(AgentConversationDO::getHumanTakeoverUserId, null)
                .set(AgentConversationDO::getHumanTakeoverTime, null)
                .set(AgentConversationDO::getPendingReplyMessageId, null)
                .set(AgentConversationDO::getPendingReplyDueTime, null));
    }

    default Long selectRiskCount() {
        return selectCount(new LambdaQueryWrapperX<AgentConversationDO>()
                .gt(AgentConversationDO::getRiskLevel, AgentConstants.RISK_LEVEL_GREEN));
    }

}
