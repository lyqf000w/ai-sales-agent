package cn.ai.sales.module.agent.controller.admin.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - AI 销冠回复审核 Response VO")
@Data
public class AgentReplyReviewRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;
    @Schema(description = "会话编号", example = "1")
    private Long conversationId;
    @Schema(description = "Contact id", example = "1")
    private Long contactId;
    @Schema(description = "客户消息编号", example = "1")
    private Long inboundMessageId;
    @Schema(description = "建议消息编号", example = "2")
    private Long suggestedMessageId;
    @Schema(description = "已发送消息编号", example = "2")
    private Long sentMessageId;
    @Schema(description = "决策类型")
    private String decisionType;
    @Schema(description = "风险等级")
    private Integer riskLevel;
    @Schema(description = "置信度")
    private BigDecimal confidence;
    @Schema(description = "模型")
    private String llmModel;
    @Schema(description = "实际生成来源")
    private String generationSource;
    @Schema(description = "实际大模型供应商")
    private String llmProvider;
    @Schema(description = "实际大模型")
    private String actualLlmModel;
    @Schema(description = "知识引用")
    private Map<String, Object> knowledgeRefs;
    @Schema(description = "规则命中")
    private Map<String, Object> guardrailHits;
    @Schema(description = "决策原因")
    private String decisionReason;
    @Schema(description = "审核状态")
    private String reviewStatus;
    @Schema(description = "审核说明")
    private String reviewNote;
    @Schema(description = "审核人")
    private Long reviewUserId;
    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;
    @Schema(description = "建议内容")
    private String suggestedContent;
    @Schema(description = "命中策略")
    private String matchedPolicy;
    @Schema(description = "审计说明")
    private String auditNote;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
