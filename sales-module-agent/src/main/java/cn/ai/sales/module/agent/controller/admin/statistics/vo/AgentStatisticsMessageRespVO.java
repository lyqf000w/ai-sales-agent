package cn.ai.sales.module.agent.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Admin - AI sales statistics message response")
@Data
public class AgentStatisticsMessageRespVO {

    @Schema(description = "Message id", example = "1")
    private Long id;
    @Schema(description = "Conversation id", example = "1")
    private Long conversationId;
    @Schema(description = "WeChat account id", example = "1")
    private Long wechatAccountId;
    @Schema(description = "Contact id", example = "1")
    private Long contactId;
    @Schema(description = "WeChat account display name")
    private String accountName;
    @Schema(description = "Contact display name")
    private String contactName;
    @Schema(description = "Direction", example = "1")
    private Integer direction;
    @Schema(description = "Sender type", example = "1")
    private Integer senderType;
    @Schema(description = "Message type", example = "1")
    private Integer messageType;
    @Schema(description = "Message content")
    private String content;
    @Schema(description = "Send status", example = "2")
    private Integer sendStatus;
    @Schema(description = "Intent")
    private String intent;
    @Schema(description = "Matched policy")
    private String matchedPolicy;
    @Schema(description = "Audit note")
    private String auditNote;
    @Schema(description = "Message time")
    private LocalDateTime messageTime;
    @Schema(description = "Create time")
    private LocalDateTime createTime;

}
