package cn.ai.sales.module.agent.dal.dataobject;

import cn.ai.sales.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("agent_contact_tag")
@KeySequence("agent_contact_tag_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentContactTagDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String name;
    private String color;
    private String description;
    private Integer sort;
    private Integer status;

}
