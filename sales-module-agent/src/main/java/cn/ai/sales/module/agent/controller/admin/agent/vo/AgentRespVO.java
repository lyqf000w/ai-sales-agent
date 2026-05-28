package cn.ai.sales.module.agent.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - AI 销冠 Agent Response VO")
@Data
public class AgentRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;
    @Schema(description = "Agent 名称")
    private String name;
    @Schema(description = "别名")
    private String aliasName;
    @Schema(description = "负责人用户编号")
    private Long ownerUserId;
    @Schema(description = "销售场景")
    private String scene;
    @Schema(description = "目标客户描述")
    private String targetCustomerDesc;
    @Schema(description = "系统提示词")
    private String systemPrompt;
    @Schema(description = "大模型供应商")
    private String llmProvider;
    @Schema(description = "大模型名称")
    private String llmModel;
    @Schema(description = "默认知识库编号")
    private Long knowledgeBaseId;
    @Schema(description = "回复模式")
    private String replyMode;
    @Schema(description = "历史兼容字段：自动回复最低置信度")
    private BigDecimal confidenceThreshold;
    @Schema(description = "历史兼容字段：单客户连续自动回复上限")
    private Integer maxContinuousAutoReply;
    @Schema(description = "历史兼容字段：静默分钟数")
    private Integer quietMinutes;
    @Schema(description = "静默秒数")
    private Integer quietSeconds;
    @Schema(description = "营业时间配置")
    private Map<String, Object> businessHours;
    @Schema(description = "回复语气")
    private String tone;
    @Schema(description = "欢迎语")
    private String welcomeMessage;
    @Schema(description = "转人工话术")
    private String handoverMessage;
    @Schema(description = "跟进策略")
    private Map<String, Object> followUpPolicy;
    @Schema(description = "素材优先级")
    private Map<String, Object> materialPriority;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "草稿版本")
    private Integer draftVersion;
    @Schema(description = "上线版本")
    private Integer onlineVersion;
    @Schema(description = "已发布配置快照")
    private Map<String, Object> publishedConfig;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
