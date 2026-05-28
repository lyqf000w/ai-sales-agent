package cn.ai.sales.module.agent.controller.admin.sensitiverule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠人工升级规则新增/修改 Request VO")
@Data
public class AgentSensitiveRuleSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "退款")
    @NotEmpty(message = "规则名称不能为空")
    private String name;

    @Schema(description = "Agent 编号；为空表示全局规则", example = "1")
    private Long agentId;

    @Schema(description = "接入应用；为空表示全部", example = "GEWE")
    private String routeApp;

    @Schema(description = "触发类型：KEYWORD、REGEX、INTENT、SENTIMENT、CUSTOMER_LEVEL、RAG_MISS、REQUEST_HUMAN",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "INTENT")
    @NotEmpty(message = "触发类型不能为空")
    private String triggerType;

    @Schema(description = "匹配方式：1 关键词，2 正则", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "匹配方式不能为空")
    private Integer matchType;

    @Schema(description = "匹配内容；关键词、正则、意图、情绪、客户等级需要填写", example = "退款")
    private String pattern;

    @Schema(description = "动作：1 人工确认，3 人工接管", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "动作不能为空")
    private Integer action;

    @Schema(description = "风险等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "风险等级不能为空")
    private Integer riskLevel;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
