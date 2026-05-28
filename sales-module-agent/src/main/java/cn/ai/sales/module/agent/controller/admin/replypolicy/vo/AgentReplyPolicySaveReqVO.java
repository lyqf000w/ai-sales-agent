package cn.ai.sales.module.agent.controller.admin.replypolicy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "Admin - personal sales assistant reply policy save request")
@Data
public class AgentReplyPolicySaveReqVO {

    @Schema(description = "WeChat account id, used when saving account default policy", example = "1")
    private Long wechatAccountId;

    @Schema(description = "Contact id, used when saving customer/session override", example = "1")
    private Long contactId;

    @Schema(description = "Conversation id, can be used to resolve contact id", example = "1")
    private Long conversationId;

    @Schema(description = "Reply mode: AUTO_REPLY, MANUAL_CONFIRM, RECORD_ONLY", example = "MANUAL_CONFIRM")
    private String replyMode;

    @Schema(description = "Quiet window in seconds. New customer messages reset the timer.", example = "90")
    private Integer quietSeconds;

    @Schema(description = "Business hours, for example {\"start\":\"08:00\",\"end\":\"22:00\"}")
    private Map<String, Object> businessHours;

}
