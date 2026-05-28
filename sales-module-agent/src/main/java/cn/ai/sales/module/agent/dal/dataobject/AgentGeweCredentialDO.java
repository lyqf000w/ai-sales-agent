package cn.ai.sales.module.agent.dal.dataobject;

import cn.ai.sales.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("agent_gewe_credential")
@KeySequence("agent_gewe_credential_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentGeweCredentialDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String name;
    private String geweApiBaseUrl;
    private String geweToken;
    private String callbackToken;
    private String callbackSecret;
    private String callbackUrl;
    private LocalDateTime callbackConfiguredTime;
    private Integer status;

}
