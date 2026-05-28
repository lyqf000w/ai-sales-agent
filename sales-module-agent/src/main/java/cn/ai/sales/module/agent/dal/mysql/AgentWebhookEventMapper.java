package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentWebhookEventPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWebhookEventDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.collection.CollUtil;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface AgentWebhookEventMapper extends BaseMapperX<AgentWebhookEventDO> {

    default PageResult<AgentWebhookEventDO> selectPage(AgentWebhookEventPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AgentWebhookEventDO>()
                .eqIfPresent(AgentWebhookEventDO::getWechatAccountId, reqVO.getWechatAccountId())
                .eqIfPresent(AgentWebhookEventDO::getEventType, reqVO.getEventType())
                .eqIfPresent(AgentWebhookEventDO::getProcessStatus, reqVO.getProcessStatus())
                .likeIfPresent(AgentWebhookEventDO::getEventId, reqVO.getEventId())
                .betweenIfPresent(AgentWebhookEventDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AgentWebhookEventDO::getId));
    }

    default AgentWebhookEventDO selectByAccountAndEventId(Long wechatAccountId, String eventId) {
        return selectOne(AgentWebhookEventDO::getWechatAccountId, wechatAccountId,
                AgentWebhookEventDO::getEventId, eventId);
    }

    default Long selectCountByCreateTimeBetween(LocalDateTime beginTime, LocalDateTime endTime) {
        return selectCount(new LambdaQueryWrapperX<AgentWebhookEventDO>()
                .ge(AgentWebhookEventDO::getCreateTime, beginTime)
                .lt(AgentWebhookEventDO::getCreateTime, endTime));
    }

    default Long selectFailedCount() {
        return selectCount(new LambdaQueryWrapperX<AgentWebhookEventDO>()
                .eq(AgentWebhookEventDO::getProcessStatus, AgentConstants.WEBHOOK_STATUS_FAILED));
    }

    default AgentWebhookEventDO selectLatest() {
        return CollUtil.getFirst(selectList(new LambdaQueryWrapperX<AgentWebhookEventDO>()
                .orderByDesc(AgentWebhookEventDO::getId)
                .last("LIMIT 1")));
    }

}
