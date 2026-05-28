package cn.ai.sales.module.agent.dal.dataobject;

import cn.ai.sales.framework.tenant.core.db.TenantBaseDO;
import cn.ai.sales.module.agent.dal.typehandler.JsonbMapTypeHandler;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Map;

@TableName(value = "agent_agent", autoResultMap = true)
@KeySequence("agent_agent_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String name;
    private String aliasName;
    private Long ownerUserId;
    private String scene;
    private String targetCustomerDesc;
    private String systemPrompt;
    private String llmProvider;
    private String llmModel;
    private Long knowledgeBaseId;
    private String replyMode;
    private BigDecimal confidenceThreshold;
    private Integer maxContinuousAutoReply;
    private Integer quietMinutes;
    private Integer quietSeconds;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> businessHours;
    private String tone;
    private String welcomeMessage;
    private String handoverMessage;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> followUpPolicy;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> materialPriority;
    private Integer status;
    private Integer draftVersion;
    private Integer onlineVersion;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> publishedConfig;

}
