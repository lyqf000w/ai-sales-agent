package cn.ai.sales.module.agent.dal.dataobject;

import cn.ai.sales.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("agent_conversation")
@KeySequence("agent_conversation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentConversationDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long agentId;
    private Long wechatAccountId;
    private Long contactId;
    private Integer status;
    private Integer riskLevel;
    private Long lastMessageId;
    private LocalDateTime lastMessageTime;
    private Long pendingReplyMessageId;
    private LocalDateTime pendingReplyDueTime;
    private Integer continuousAutoReplyCount;
    private Long humanTakeoverUserId;
    private LocalDateTime humanTakeoverTime;

}
