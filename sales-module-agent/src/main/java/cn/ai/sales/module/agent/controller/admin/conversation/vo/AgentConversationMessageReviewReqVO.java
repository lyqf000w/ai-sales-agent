package cn.ai.sales.module.agent.controller.admin.conversation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠会话消息审核 Request VO")
@Data
public class AgentConversationMessageReviewReqVO {

    @Schema(description = "消息编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "消息编号不能为空")
    private Long messageId;

    @Schema(description = "修改后的回复内容")
    private String content;

    @Schema(description = "驳回原因")
    private String reason;

}
