package cn.ai.sales.module.agent.controller.admin.conversation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠恢复原回复策略 Request VO")
@Data
public class AgentConversationRestoreOriginalPolicyReqVO {

    @Schema(description = "会话编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "会话编号不能为空")
    private Long conversationId;

}
