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

@TableName(value = "agent_wechat_contact", autoResultMap = true)
@KeySequence("agent_wechat_contact_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentWechatContactDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long wechatAccountId;
    private String externalUserId;
    private String wechatId;
    private String nickname;
    private String remark;
    private String avatar;
    private Integer customerLevel;
    private Long ownerUserId;
    private Integer riskLevel;
    private LocalDateTime lastMessageTime;
    private Integer lastConversationStatus;
    private String replyMode;
    private String purchaseIntention;
    private String salesStage;
    private String customerSentiment;
    private String followUpPriority;
    private BigDecimal confidenceThreshold;
    private Integer maxContinuousAutoReply;
    private Integer quietMinutes;
    private Integer quietSeconds;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> businessHours;

}
