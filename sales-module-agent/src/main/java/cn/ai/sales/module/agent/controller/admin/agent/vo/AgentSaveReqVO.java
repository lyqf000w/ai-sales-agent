package cn.ai.sales.module.agent.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "管理后台 - AI 销冠 Agent 新增/修改 Request VO")
@Data
public class AgentSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "Agent 名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "默认销售助手")
    @NotEmpty(message = "Agent 名称不能为空")
    private String name;

    @Schema(description = "别名", example = "小销")
    private String aliasName;

    @Schema(description = "负责人用户编号", example = "1")
    private Long ownerUserId;

    @Schema(description = "销售场景", example = "SaaS 线索转化")
    private String scene;

    @Schema(description = "目标客户描述")
    private String targetCustomerDesc;

    @Schema(description = "系统提示词")
    private String systemPrompt;

    @Schema(description = "大模型供应商", example = "DEEPSEEK")
    private String llmProvider;

    @Schema(description = "大模型名称", example = "deepseek-v4-pro")
    private String llmModel;

    @Schema(description = "默认知识库编号", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "历史兼容回复模式，实际回复策略在微信号/好友配置", example = "MANUAL_CONFIRM")
    private String replyMode;

    @Schema(description = "历史兼容字段：自动回复最低置信度", example = "0.70")
    private BigDecimal confidenceThreshold;

    @Schema(description = "历史兼容字段：单客户连续自动回复上限", example = "3")
    private Integer maxContinuousAutoReply;

    @Schema(description = "历史兼容字段：静默分钟数", example = "0")
    private Integer quietMinutes;

    @Schema(description = "静默秒数", example = "30")
    private Integer quietSeconds;

    @Schema(description = "营业时间配置，例如 {\"start\":\"09:00\",\"end\":\"18:00\"}")
    private Map<String, Object> businessHours;

    @Schema(description = "回复语气", example = "专业、克制、简洁")
    private String tone;

    @Schema(description = "欢迎语")
    private String welcomeMessage;

    @Schema(description = "转人工话术")
    private String handoverMessage;

    @Schema(description = "跟进策略，例如 {\"firstDelayMinutes\":30,\"maxFollowUps\":3}")
    private Map<String, Object> followUpPolicy;

    @Schema(description = "素材优先级，例如 {\"primary\":\"knowledge\",\"secondary\":\"manual\"}")
    private Map<String, Object> materialPriority;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

}
