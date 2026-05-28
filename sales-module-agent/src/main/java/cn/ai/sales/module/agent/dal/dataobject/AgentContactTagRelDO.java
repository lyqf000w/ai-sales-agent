package cn.ai.sales.module.agent.dal.dataobject;

import cn.ai.sales.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("agent_contact_tag_rel")
@KeySequence("agent_contact_tag_rel_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentContactTagRelDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long contactId;
    private Long tagId;

}
