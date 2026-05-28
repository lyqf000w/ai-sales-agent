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
import java.time.LocalDateTime;
import java.util.Map;

@TableName(value = "agent_reply_decision", autoResultMap = true)
@KeySequence("agent_reply_decision_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentReplyDecisionDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long conversationId;
    private Long inboundMessageId;
    private Long suggestedMessageId;
    private Long sentMessageId;
    private String decisionType;
    private Integer riskLevel;
    private BigDecimal confidence;
    private String llmModel;
    private String promptSnapshot;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> contextSnapshot;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> knowledgeRefs;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> guardrailHits;
    private String decisionReason;
    private String reviewStatus;
    private String reviewNote;
    private Long reviewUserId;
    private LocalDateTime reviewTime;

}
