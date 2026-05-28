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

@TableName(value = "agent_config_version", autoResultMap = true)
@KeySequence("agent_config_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentConfigVersionDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long agentId;
    private Integer version;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> configSnapshot;
    private String changeSummary;
    private Long publishUserId;
    private LocalDateTime publishTime;

}
