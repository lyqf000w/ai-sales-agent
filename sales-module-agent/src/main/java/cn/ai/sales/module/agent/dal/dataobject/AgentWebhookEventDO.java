package cn.ai.sales.module.agent.dal.dataobject;

import cn.ai.sales.framework.tenant.core.db.TenantBaseDO;
import cn.ai.sales.module.agent.dal.typehandler.JsonbMapTypeHandler;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@TableName(value = "agent_webhook_event", autoResultMap = true)
@KeySequence("agent_webhook_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentWebhookEventDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long wechatAccountId;
    private String eventId;
    private String eventType;
    private Boolean signatureValid;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> rawPayload;
    private Integer processStatus;
    private String errorMessage;

}
