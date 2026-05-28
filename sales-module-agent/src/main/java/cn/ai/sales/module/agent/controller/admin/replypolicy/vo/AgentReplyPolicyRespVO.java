package cn.ai.sales.module.agent.controller.admin.replypolicy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "Admin - personal sales assistant resolved reply policy")
@Data
public class AgentReplyPolicyRespVO {

    private Long wechatAccountId;
    private Long contactId;
    private Long conversationId;
    private String replyMode;
    private Integer quietSeconds;
    private Map<String, Object> businessHours;
    private String source;

}
