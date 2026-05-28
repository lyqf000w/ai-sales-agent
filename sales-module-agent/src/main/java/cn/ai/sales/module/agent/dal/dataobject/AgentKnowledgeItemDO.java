package cn.ai.sales.module.agent.dal.dataobject;

import cn.ai.sales.framework.tenant.core.db.TenantBaseDO;
import cn.ai.sales.module.agent.dal.typehandler.PgVectorTypeHandler;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@TableName(value = "agent_knowledge_item", autoResultMap = true)
@KeySequence("agent_knowledge_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentKnowledgeItemDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long knowledgeBaseId;
    private String title;
    private String productName;
    private String category;
    private String keywords;
    private String question;
    private String answer;
    @TableField(typeHandler = PgVectorTypeHandler.class)
    private List<Double> embedding;
    private String embeddingStatus;
    private Integer sort;
    private Integer status;
    @TableField(exist = false)
    private Double distance;

}
