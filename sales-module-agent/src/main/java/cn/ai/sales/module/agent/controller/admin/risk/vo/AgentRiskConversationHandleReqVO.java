package cn.ai.sales.module.agent.controller.admin.risk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠风险会话处理 Request VO")
@Data
public class AgentRiskConversationHandleReqVO {

    @Schema(description = "会话编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "会话编号不能为空")
    private Long conversationId;

}
