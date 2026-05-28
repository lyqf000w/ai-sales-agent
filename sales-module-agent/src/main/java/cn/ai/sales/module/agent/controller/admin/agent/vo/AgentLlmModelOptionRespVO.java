package cn.ai.sales.module.agent.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - AI 销冠大模型选项 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentLlmModelOptionRespVO {

    @Schema(description = "大模型供应商", example = "DEEPSEEK")
    private String provider;
    @Schema(description = "模型名称", example = "deepseek-v4-pro")
    private String model;
    @Schema(description = "展示名称", example = "DeepSeek-V4-Pro")
    private String label;
    @Schema(description = "是否默认模型")
    private Boolean defaultModel;

}
