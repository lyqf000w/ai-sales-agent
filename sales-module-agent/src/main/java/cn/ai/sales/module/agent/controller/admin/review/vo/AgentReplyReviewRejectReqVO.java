package cn.ai.sales.module.agent.controller.admin.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠回复审核驳回 Request VO")
@Data
public class AgentReplyReviewRejectReqVO {

    @Schema(description = "回复决策编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "回复决策编号不能为空")
    private Long decisionId;

    @Schema(description = "驳回原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "驳回原因不能为空")
    private String reason;

}
