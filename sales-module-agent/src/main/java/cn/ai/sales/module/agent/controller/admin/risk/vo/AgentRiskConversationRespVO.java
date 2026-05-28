package cn.ai.sales.module.agent.controller.admin.risk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 销冠风险会话 Response VO")
@Data
public class AgentRiskConversationRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;
    @Schema(description = "Agent 编号", example = "1")
    private Long agentId;
    @Schema(description = "微信账号编号", example = "1")
    private Long wechatAccountId;
    @Schema(description = "微信好友编号", example = "1")
    private Long contactId;
    @Schema(description = "会话状态", example = "2")
    private Integer status;
    @Schema(description = "风险等级", example = "2")
    private Integer riskLevel;
    @Schema(description = "最近消息编号", example = "1")
    private Long lastMessageId;
    @Schema(description = "最近消息时间")
    private LocalDateTime lastMessageTime;
    @Schema(description = "连续自动回复次数", example = "3")
    private Integer continuousAutoReplyCount;
    @Schema(description = "人工接管人", example = "1")
    private Long humanTakeoverUserId;
    @Schema(description = "人工接管时间")
    private LocalDateTime humanTakeoverTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
