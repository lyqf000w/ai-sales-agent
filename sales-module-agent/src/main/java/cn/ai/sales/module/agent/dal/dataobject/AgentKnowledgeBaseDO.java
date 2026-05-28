package cn.ai.sales.module.agent.dal.dataobject;

import cn.ai.sales.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("agent_knowledge_base")
@KeySequence("agent_knowledge_base_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentKnowledgeBaseDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String name;
    private String description;
    private Integer status;

}
