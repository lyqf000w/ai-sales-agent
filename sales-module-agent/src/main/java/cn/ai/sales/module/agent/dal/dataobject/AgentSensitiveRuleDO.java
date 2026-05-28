package cn.ai.sales.module.agent.dal.dataobject;

import cn.ai.sales.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("agent_sensitive_rule")
@KeySequence("agent_sensitive_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentSensitiveRuleDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long agentId;
    private String routeApp;
    private String name;
    private String triggerType;
    private Integer matchType;
    private String pattern;
    private Integer action;
    private Integer riskLevel;
    private Integer sort;
    private Integer status;
    private String remark;

}
