package cn.ai.sales.module.agent.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - AI 销冠运营统计 Response VO")
@Data
public class AgentStatisticsSummaryRespVO {

    @Schema(description = "今日消息数")
    private Long todayMessageCount;

    @Schema(description = "今日自动回复数")
    private Long todayAutoReplyCount;

    @Schema(description = "待审核回复数")
    private Long pendingReviewCount;

    @Schema(description = "风险会话数")
    private Long riskConversationCount;

    @Schema(description = "购买意愿分布")
    private List<AgentStatisticsDimensionRespVO> purchaseIntentionStats;

    @Schema(description = "销售阶段分布")
    private List<AgentStatisticsDimensionRespVO> salesStageStats;

    @Schema(description = "客户情绪分布")
    private List<AgentStatisticsDimensionRespVO> customerSentimentStats;

    @Schema(description = "跟进优先级分布")
    private List<AgentStatisticsDimensionRespVO> followUpPriorityStats;

}
