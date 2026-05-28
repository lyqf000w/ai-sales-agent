package cn.ai.sales.module.agent.controller.admin.review.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠回复审核分页 Request VO")
@Data
public class AgentReplyReviewPageReqVO extends PageParam {

    @Schema(description = "会话编号", example = "1")
    private Long conversationId;

    @Schema(description = "审核状态", example = "PENDING")
    private String reviewStatus;

    @Schema(description = "风险等级", example = "1")
    private Integer riskLevel;

}
