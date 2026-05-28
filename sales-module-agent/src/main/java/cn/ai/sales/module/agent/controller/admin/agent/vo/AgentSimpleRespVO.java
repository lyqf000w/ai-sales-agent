package cn.ai.sales.module.agent.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠 Agent 精简 Response VO")
@Data
public class AgentSimpleRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;
    @Schema(description = "Agent 名称")
    private String name;
    @Schema(description = "别名")
    private String aliasName;
    @Schema(description = "销售场景")
    private String scene;
    @Schema(description = "大模型供应商")
    private String llmProvider;
    @Schema(description = "大模型名称")
    private String llmModel;

}
