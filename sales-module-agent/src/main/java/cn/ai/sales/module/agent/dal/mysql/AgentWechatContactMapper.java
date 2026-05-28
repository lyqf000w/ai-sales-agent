package cn.ai.sales.module.agent.dal.mysql;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.framework.tenant.core.context.TenantContextHolder;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactPageReqVO;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsDimensionRespVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.util.StrUtil;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface AgentWechatContactMapper extends BaseMapperX<AgentWechatContactDO> {

    default PageResult<AgentWechatContactDO> selectPage(AgentWechatContactPageReqVO reqVO) {
        LambdaQueryWrapperX<AgentWechatContactDO> query = buildBaseQuery(reqVO);
        filterMisclassifiedChatroomMemberContacts(query);
        return selectPage(reqVO, orderByRecentMessageLast(query));
    }

    default PageResult<AgentWechatContactDO> selectConversationQueuePage(AgentWechatContactPageReqVO reqVO) {
        LambdaQueryWrapperX<AgentWechatContactDO> query = buildBaseQuery(reqVO);
        filterMisclassifiedChatroomMemberContacts(query);
        if (AgentConstants.CONVERSATION_QUEUE_PENDING_REVIEW.equals(reqVO.getQueueType())) {
            filterContactsWithPendingReview(query);
        } else if (AgentConstants.CONVERSATION_QUEUE_TAKEOVER.equals(reqVO.getQueueType())) {
            filterContactsWithConversationStatus(query, AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
        } else if (AgentConstants.CONVERSATION_QUEUE_RISK.equals(reqVO.getQueueType())) {
            filterContactsWithRiskConversation(query);
        } else if (AgentConstants.CONVERSATION_QUEUE_FOCUS.equals(reqVO.getQueueType())) {
            query.eq(AgentWechatContactDO::getFollowUpPriority, AgentConstants.FOLLOW_UP_PRIORITY_FOCUS);
        } else if (AgentConstants.CONVERSATION_QUEUE_URGENT.equals(reqVO.getQueueType())) {
            query.eq(AgentWechatContactDO::getFollowUpPriority, AgentConstants.FOLLOW_UP_PRIORITY_URGENT);
        }
        return selectPage(reqVO, orderByRecentMessageLast(query));
    }

    private static LambdaQueryWrapperX<AgentWechatContactDO> orderByRecentMessageLast(
            LambdaQueryWrapperX<AgentWechatContactDO> query) {
        return query.last("ORDER BY last_message_time IS NULL, last_message_time DESC, id DESC");
    }

    private static LambdaQueryWrapperX<AgentWechatContactDO> buildBaseQuery(AgentWechatContactPageReqVO reqVO) {
        LambdaQueryWrapperX<AgentWechatContactDO> query = new LambdaQueryWrapperX<AgentWechatContactDO>()
                .eqIfPresent(AgentWechatContactDO::getWechatAccountId, reqVO.getWechatAccountId())
                .eqIfPresent(AgentWechatContactDO::getId, reqVO.getContactId())
                .eqIfPresent(AgentWechatContactDO::getCustomerLevel, reqVO.getCustomerLevel())
                .eqIfPresent(AgentWechatContactDO::getRiskLevel, reqVO.getRiskLevel())
                .eqIfPresent(AgentWechatContactDO::getPurchaseIntention, reqVO.getPurchaseIntention())
                .eqIfPresent(AgentWechatContactDO::getSalesStage, reqVO.getSalesStage())
                .eqIfPresent(AgentWechatContactDO::getCustomerSentiment, reqVO.getCustomerSentiment())
                .eqIfPresent(AgentWechatContactDO::getFollowUpPriority, reqVO.getFollowUpPriority());
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            query.and(wrapper -> wrapper
                    .like(AgentWechatContactDO::getNickname, reqVO.getKeyword())
                    .or()
                    .like(AgentWechatContactDO::getRemark, reqVO.getKeyword())
                    .or()
                    .like(AgentWechatContactDO::getWechatId, reqVO.getKeyword())
                    .or()
                    .like(AgentWechatContactDO::getExternalUserId, reqVO.getKeyword()));
        }
        if (reqVO.getTagId() != null) {
            filterContactsWithTag(query, reqVO.getTagId());
        }
        return query;
    }

    private static void filterContactsWithTag(LambdaQueryWrapperX<AgentWechatContactDO> query, Long tagId) {
        query.inSql(AgentWechatContactDO::getId,
                "SELECT contact_id FROM agent_contact_tag_rel WHERE deleted = 0 AND tenant_id = "
                        + TenantContextHolder.getRequiredTenantId()
                        + " AND tag_id = " + tagId);
    }

    private static void filterContactsWithConversation(LambdaQueryWrapperX<AgentWechatContactDO> query) {
        query.inSql(AgentWechatContactDO::getId,
                "SELECT contact_id FROM agent_conversation WHERE deleted = 0 AND tenant_id = "
                        + TenantContextHolder.getRequiredTenantId());
    }

    private static void filterMisclassifiedChatroomMemberContacts(LambdaQueryWrapperX<AgentWechatContactDO> query) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return;
        }
        query.notExists("SELECT 1 FROM agent_message m"
                + " WHERE m.deleted = 0"
                + " AND m.tenant_id = " + tenantId
                + " AND m.contact_id = agent_wechat_contact.id"
                + " AND agent_wechat_contact.external_user_id NOT LIKE '%@chatroom'"
                + " AND (CAST(m.raw_payload AS TEXT) LIKE '%@chatroom%'"
                + " OR COALESCE(CAST(m.raw_payload AS TEXT), '') LIKE '%group_msg_event%'"
                + " OR COALESCE(CAST(m.raw_payload AS TEXT), '') LIKE '%fromGroup%'"
                + " OR COALESCE(m.content, '') LIKE 'wxid\\_%:%')"
                + " AND NOT EXISTS (SELECT 1 FROM agent_message direct_m"
                + " WHERE direct_m.deleted = 0"
                + " AND direct_m.tenant_id = " + tenantId
                + " AND direct_m.contact_id = agent_wechat_contact.id"
                + " AND direct_m.direction = " + AgentConstants.MESSAGE_DIRECTION_INBOUND
                + " AND COALESCE(CAST(direct_m.raw_payload AS TEXT), '') NOT LIKE '%@chatroom%'"
                + " AND COALESCE(direct_m.content, '') NOT LIKE 'wxid\\_%:%')");
    }

    private static void filterContactsWithPendingReview(LambdaQueryWrapperX<AgentWechatContactDO> query) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        query.and(wrapper -> wrapper
                .inSql(AgentWechatContactDO::getId,
                        "SELECT DISTINCT c.contact_id FROM agent_reply_decision d "
                                + "JOIN agent_conversation c ON c.id = d.conversation_id "
                                + "AND c.deleted = 0 AND c.tenant_id = d.tenant_id "
                                + "JOIN agent_message m ON m.id = d.suggested_message_id "
                                + "AND m.deleted = 0 AND m.tenant_id = d.tenant_id "
                                + "WHERE d.deleted = 0 AND d.tenant_id = " + tenantId
                                + " AND d.review_status = '" + AgentConstants.REVIEW_STATUS_PENDING + "'"
                                + " AND m.send_status = " + AgentConstants.SEND_STATUS_PENDING_REVIEW)
                .or()
                .inSql(AgentWechatContactDO::getId,
                        "SELECT contact_id FROM agent_conversation WHERE deleted = 0 AND tenant_id = "
                                + tenantId
                                + " AND status = " + AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM));
    }

    private static void filterContactsWithConversationStatus(LambdaQueryWrapperX<AgentWechatContactDO> query,
                                                             Integer status) {
        query.inSql(AgentWechatContactDO::getId,
                "SELECT contact_id FROM agent_conversation WHERE deleted = 0 AND tenant_id = "
                        + TenantContextHolder.getRequiredTenantId()
                        + " AND status = " + status);
    }

    private static void filterContactsWithRiskConversation(LambdaQueryWrapperX<AgentWechatContactDO> query) {
        query.inSql(AgentWechatContactDO::getId,
                "SELECT contact_id FROM agent_conversation WHERE deleted = 0 AND tenant_id = "
                        + TenantContextHolder.getRequiredTenantId()
                        + " AND risk_level > " + AgentConstants.RISK_LEVEL_GREEN);
    }

    default AgentWechatContactDO selectByAccountAndExternalUserId(Long wechatAccountId, String externalUserId) {
        return selectOne(AgentWechatContactDO::getWechatAccountId, wechatAccountId,
                AgentWechatContactDO::getExternalUserId, externalUserId);
    }

    default List<AgentWechatContactDO> selectListByWechatAccountId(Long wechatAccountId) {
        return selectList(AgentWechatContactDO::getWechatAccountId, wechatAccountId);
    }

    default void updateReplyPolicyById(AgentWechatContactDO updateObj) {
        update(null, new LambdaUpdateWrapper<AgentWechatContactDO>()
                .eq(AgentWechatContactDO::getId, updateObj.getId())
                .set(AgentWechatContactDO::getReplyMode, updateObj.getReplyMode())
                .set(AgentWechatContactDO::getQuietSeconds, updateObj.getQuietSeconds())
                .set(AgentWechatContactDO::getBusinessHours, updateObj.getBusinessHours(),
                        "typeHandler=cn.ai.sales.module.agent.dal.typehandler.JsonbMapTypeHandler"));
    }

    default void resetConversationStateForAutoReply(Long contactId) {
        update(null, new LambdaUpdateWrapper<AgentWechatContactDO>()
                .eq(AgentWechatContactDO::getId, contactId)
                .set(AgentWechatContactDO::getRiskLevel, AgentConstants.RISK_LEVEL_GREEN)
                .set(AgentWechatContactDO::getLastConversationStatus, AgentConstants.CONVERSATION_STATUS_OPEN));
    }

    default void resetConversationStateForAutoReplyByWechatAccountId(Long wechatAccountId) {
        update(null, new LambdaUpdateWrapper<AgentWechatContactDO>()
                .eq(AgentWechatContactDO::getWechatAccountId, wechatAccountId)
                .set(AgentWechatContactDO::getRiskLevel, AgentConstants.RISK_LEVEL_GREEN)
                .set(AgentWechatContactDO::getLastConversationStatus, AgentConstants.CONVERSATION_STATUS_OPEN));
    }

    default List<AgentStatisticsDimensionRespVO> selectPurchaseIntentionStats() {
        return selectDimensionStats("purchase_intention");
    }

    default List<AgentStatisticsDimensionRespVO> selectSalesStageStats() {
        return selectDimensionStats("sales_stage");
    }

    default List<AgentStatisticsDimensionRespVO> selectCustomerSentimentStats() {
        return selectDimensionStats("customer_sentiment");
    }

    default List<AgentStatisticsDimensionRespVO> selectFollowUpPriorityStats() {
        return selectDimensionStats("follow_up_priority");
    }

    private List<AgentStatisticsDimensionRespVO> selectDimensionStats(String column) {
        QueryWrapper<AgentWechatContactDO> query = new QueryWrapper<>();
        query.select("COALESCE(" + column + ", 'UNKNOWN') AS code", "COUNT(*) AS count")
                .groupBy(column);
        return toDimensionStats(selectMaps(query));
    }

    private static List<AgentStatisticsDimensionRespVO> toDimensionStats(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> {
            AgentStatisticsDimensionRespVO respVO = new AgentStatisticsDimensionRespVO();
            respVO.setCode(String.valueOf(row.get("code")));
            Object countValue = row.get("count");
            respVO.setCount(countValue instanceof Number number ? number.longValue()
                    : Long.parseLong(String.valueOf(countValue)));
            return respVO;
        }).toList();
    }

}
