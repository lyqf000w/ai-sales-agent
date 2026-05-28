package cn.ai.sales.module.agent.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠 Agent 发布 Request VO")
@Data
public class AgentPublishReqVO {

    @Schema(description = "Agent 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "Agent 编号不能为空")
    private Long agentId;

    @Schema(description = "变更说明", example = "更新报价口径")
    private String changeSummary;

}
