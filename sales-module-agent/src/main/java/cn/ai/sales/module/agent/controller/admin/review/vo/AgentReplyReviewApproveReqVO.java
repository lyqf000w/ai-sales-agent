package cn.ai.sales.module.agent.controller.admin.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠回复审核通过 Request VO")
@Data
public class AgentReplyReviewApproveReqVO {

    @Schema(description = "回复决策编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "回复决策编号不能为空")
    private Long decisionId;

    @Schema(description = "修改后的回复内容")
    private String content;

}
