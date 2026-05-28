package cn.ai.sales.module.agent.controller.admin.conversation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 销冠消息 Response VO")
@Data
public class AgentMessageRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;
    @Schema(description = "会话编号", example = "1")
    private Long conversationId;
    @Schema(description = "方向", example = "1")
    private Integer direction;
    @Schema(description = "发送方类型", example = "1")
    private Integer senderType;
    @Schema(description = "sender display name")
    private String senderDisplayName;
    @Schema(description = "消息类型", example = "1")
    private Integer messageType;
    @Schema(description = "消息内容")
    private String content;
    @Schema(description = "media URL")
    private String mediaUrl;
    @Schema(description = "media AES key")
    private String mediaAesKey;
    @Schema(description = "thumbnail URL")
    private String thumbUrl;
    @Schema(description = "media name")
    private String mediaName;
    @Schema(description = "media duration millis")
    private Integer mediaDurationMillis;
    @Schema(description = "Gewe 消息编号")
    private String geweMessageId;
    @Schema(description = "发送状态", example = "0")
    private Integer sendStatus;
    @Schema(description = "识别意图")
    private String intent;
    @Schema(description = "命中策略")
    private String matchedPolicy;
    @Schema(description = "审计说明")
    private String auditNote;
    @Schema(description = "操作人", example = "1")
    private Long operatorUserId;
    @Schema(description = "消息时间")
    private LocalDateTime messageTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
