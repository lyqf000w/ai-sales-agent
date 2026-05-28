package cn.ai.sales.module.agent.controller.admin.contact.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠微信好友销售洞察更新 Request VO")
@Data
public class AgentWechatContactUpdateSalesInsightReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "购买意愿", example = "HIGH")
    private String purchaseIntention;

    @Schema(description = "销售阶段", example = "QUOTE_NEGOTIATION")
    private String salesStage;

    @Schema(description = "客户情绪", example = "POSITIVE")
    private String customerSentiment;

    @Schema(description = "跟进优先级", example = "URGENT")
    private String followUpPriority;

}
