package cn.ai.sales.module.agent.dal.dataobject;

import cn.ai.sales.framework.tenant.core.db.TenantBaseDO;
import cn.ai.sales.module.agent.dal.typehandler.JsonbMapTypeHandler;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

@TableName(value = "agent_message", autoResultMap = true)
@KeySequence("agent_message_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentMessageDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long conversationId;
    private Long wechatAccountId;
    private Long contactId;
    private Integer direction;
    private Integer senderType;
    private Integer messageType;
    private String content;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> rawPayload;
    private String geweMessageId;
    private Integer sendStatus;
    private String intent;
    private String matchedPolicy;
    private String auditNote;
    private Long operatorUserId;
    private LocalDateTime messageTime;

}
