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

@TableName(value = "agent_wechat_bind_session", autoResultMap = true)
@KeySequence("agent_wechat_bind_session_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentWechatBindSessionDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long credentialId;
    private Long agentId;
    private Long ownerUserId;
    private String appId;
    private String uuid;
    private String qrData;
    private String qrImgBase64;
    private String verifyUrl;
    private String nickName;
    private String avatar;
    private String status;
    private LocalDateTime expiresAt;
    private Long bindAccountId;
    private String errorMessage;
    @TableField(typeHandler = JsonbMapTypeHandler.class)
    private Map<String, Object> rawResponse;

}
