package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsMessagePageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AgentMessageMapper extends BaseMapperX<AgentMessageDO> {

    default List<AgentMessageDO> selectListByConversationId(Long conversationId) {
        return selectList(new LambdaQueryWrapper<AgentMessageDO>()
                .eq(AgentMessageDO::getConversationId, conversationId)
                .orderByAsc(AgentMessageDO::getMessageTime)
                .orderByAsc(AgentMessageDO::getId));
    }

    default AgentMessageDO selectByGeweMessageId(Long wechatAccountId, Long contactId, String geweMessageId) {
        return selectOne(AgentMessageDO::getWechatAccountId, wechatAccountId,
                AgentMessageDO::getContactId, contactId,
                AgentMessageDO::getGeweMessageId, geweMessageId);
    }

    default Long selectCountByMessageTimeBetween(LocalDateTime beginTime, LocalDateTime endTime) {
        return selectCount(new LambdaQueryWrapper<AgentMessageDO>()
                .ge(AgentMessageDO::getMessageTime, beginTime)
                .lt(AgentMessageDO::getMessageTime, endTime));
    }

    default Long selectAutoReplyCountByMessageTimeBetween(LocalDateTime beginTime, LocalDateTime endTime) {
        return selectCount(new LambdaQueryWrapper<AgentMessageDO>()
                .eq(AgentMessageDO::getDirection, AgentConstants.MESSAGE_DIRECTION_OUTBOUND)
                .eq(AgentMessageDO::getSenderType, AgentConstants.SENDER_AI_AGENT)
                .eq(AgentMessageDO::getSendStatus, AgentConstants.SEND_STATUS_SENT)
                .ge(AgentMessageDO::getMessageTime, beginTime)
                .lt(AgentMessageDO::getMessageTime, endTime));
    }

    default PageResult<AgentMessageDO> selectStatisticsMessagePage(AgentStatisticsMessagePageReqVO reqVO,
                                                                   LocalDateTime beginTime,
                                                                   LocalDateTime endTime,
                                                                   boolean autoReplyOnly) {
        LambdaQueryWrapperX<AgentMessageDO> query = new LambdaQueryWrapperX<>();
        query.ge(AgentMessageDO::getMessageTime, beginTime);
        query.lt(AgentMessageDO::getMessageTime, endTime);
        if (autoReplyOnly) {
            query.eq(AgentMessageDO::getDirection, AgentConstants.MESSAGE_DIRECTION_OUTBOUND)
                    .eq(AgentMessageDO::getSenderType, AgentConstants.SENDER_AI_AGENT)
                    .eq(AgentMessageDO::getSendStatus, AgentConstants.SEND_STATUS_SENT);
        }
        return selectPage(reqVO, query.orderByDesc(AgentMessageDO::getMessageTime)
                .orderByDesc(AgentMessageDO::getId));
    }

}
