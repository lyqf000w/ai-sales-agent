package cn.ai.sales.module.agent.controller.admin.diagnostics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Admin - personal sales assistant diagnostics summary")
@Data
public class AgentDiagnosticsSummaryRespVO {

    private Boolean deepSeekEnabled;
    private Boolean deepSeekApiKeyConfigured;
    private String deepSeekUrl;
    private String deepSeekModel;
    private Long wechatAccountCount;
    private Long onlineWechatAccountCount;
    private Long geweCredentialCount;
    private Long enabledGeweCredentialCount;
    private Long todayWebhookCount;
    private Long failedWebhookCount;
    private Long pendingReviewCount;
    private Long riskConversationCount;
    private Long recentDeepSeekReplyCount;
    private Long recentKnowledgeReplyCount;
    private Long recentFallbackReplyCount;
    private LocalDateTime lastWebhookTime;
    private LocalDateTime lastReplyDecisionTime;
    private List<AgentDiagnosticsGenerationRespVO> recentGenerations;

}
