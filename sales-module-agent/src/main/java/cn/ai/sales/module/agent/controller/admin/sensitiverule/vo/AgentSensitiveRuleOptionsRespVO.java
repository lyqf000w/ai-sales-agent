package cn.ai.sales.module.agent.controller.admin.sensitiverule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - AI 销冠人工升级规则选项 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentSensitiveRuleOptionsRespVO {

    @Schema(description = "触发类型选项")
    private List<Option> triggerTypes;
    @Schema(description = "动作选项")
    private List<Option> actions;
    @Schema(description = "风险等级选项")
    private List<Option> riskLevels;
    @Schema(description = "意图选项")
    private List<Option> intents;
    @Schema(description = "情绪选项")
    private List<Option> sentiments;
    @Schema(description = "客户等级选项")
    private List<Option> customerLevels;

    @Schema(description = "人工升级规则通用选项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {

        @Schema(description = "选项值", example = "INTENT")
        private String value;
        @Schema(description = "展示名称", example = "意图识别")
        private String label;
        @Schema(description = "说明")
        private String description;

    }

}
