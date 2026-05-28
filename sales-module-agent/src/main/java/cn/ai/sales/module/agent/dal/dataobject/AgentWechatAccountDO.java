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

@TableName(value = "agent_wechat_account", autoResultMap = true)
@KeySequence("agent_wechat_account_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentWechatAccountDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long geweCredentialId;
    private Long agentId;
    private Long knowledgeBaseId;
    private Long ownerUserId;
    private String geweAppId;
    private String geweAccountId;
    private String wechatId;
    private String nickname;
    private String avatar;
    private String mobile;
    private String callbackToken;
    private String callbackSecret;
    private String callbackUrl;
    private String geweApiBaseUrl;
    private String geweToken;
    private String replyMode;
    private BigDecimal confidenceThreshold;
    private Integer maxContinuousAutoReply;
    private Integer quietMinutes;
    private Integer quietSeconds;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> businessHours;
    private Integer loginStatus;
    private Integer status;
    private LocalDateTime lastHeartbeatTime;

}
