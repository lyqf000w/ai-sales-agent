package cn.ai.sales.module.agent.controller.admin.contact.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - AI 销冠微信好友 Response VO")
@Data
public class AgentWechatContactRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;
    @Schema(description = "微信账号编号", example = "1")
    private Long wechatAccountId;
    @Schema(description = "外部好友标识", example = "wxid_customer")
    private String externalUserId;
    @Schema(description = "微信号", example = "wxid_customer")
    private String wechatId;
    @Schema(description = "昵称", example = "张三")
    private String nickname;
    @Schema(description = "备注", example = "重点客户")
    private String remark;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "客户等级", example = "1")
    private Integer customerLevel;
    @Schema(description = "负责人用户编号", example = "1")
    private Long ownerUserId;
    @Schema(description = "风险等级", example = "0")
    private Integer riskLevel;
    @Schema(description = "最近消息时间")
    private LocalDateTime lastMessageTime;
    @Schema(description = "最近会话状态", example = "0")
    private Integer lastConversationStatus;
    @Schema(description = "好友回复模式覆盖，空表示继承微信号策略")
    private String replyMode;
    @Schema(description = "购买意愿")
    private String purchaseIntention;
    @Schema(description = "销售阶段")
    private String salesStage;
    @Schema(description = "客户情绪")
    private String customerSentiment;
    @Schema(description = "跟进优先级")
    private String followUpPriority;
    @Schema(description = "历史兼容字段：好友自动回复最低置信度覆盖，空表示继承")
    private BigDecimal confidenceThreshold;
    @Schema(description = "历史兼容字段：好友连续自动回复上限覆盖，空表示继承")
    private Integer maxContinuousAutoReply;
    @Schema(description = "历史兼容字段：好友静默分钟数覆盖，空表示继承")
    private Integer quietMinutes;
    @Schema(description = "好友静默秒数覆盖，空表示继承")
    private Integer quietSeconds;
    @Schema(description = "好友营业时间覆盖，空表示继承")
    private Map<String, Object> businessHours;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
