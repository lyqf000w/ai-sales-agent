package cn.ai.sales.module.agent.controller.admin.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - AI 销冠微信账号 Response VO")
@Data
public class AgentWechatAccountRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;
    @Schema(description = "GeWe 凭证编号", example = "1")
    private Long geweCredentialId;
    @Schema(description = "GeWe 凭证名称", example = "默认 GeWe")
    private String geweCredentialName;
    @Schema(description = "绑定 Agent 编号", example = "1")
    private Long agentId;
    @Schema(description = "覆盖知识库编号，空表示使用 Agent 默认知识库", example = "1")
    private Long knowledgeBaseId;
    @Schema(description = "负责人用户编号", example = "1")
    private Long ownerUserId;
    @Schema(description = "Gewe Appid 或设备 ID", example = "wx-app-001")
    private String geweAppId;
    @Schema(description = "Gewe 账号标识", example = "gewe-account-001")
    private String geweAccountId;
    @Schema(description = "微信号", example = "wxid_owner")
    private String wechatId;
    @Schema(description = "微信昵称", example = "销售微信 1")
    private String nickname;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "手机号")
    private String mobile;
    @Schema(description = "回调地址")
    private String callbackUrl;
    @Schema(description = "Gewe API 地址")
    private String geweApiBaseUrl;
    @Schema(description = "是否已配置 Gewe Token")
    private Boolean geweTokenConfigured;
    @Schema(description = "默认回复模式")
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
    @Schema(description = "登录状态", example = "1")
    private Integer loginStatus;
    @Schema(description = "账号状态", example = "0")
    private Integer status;
    @Schema(description = "最近心跳时间")
    private LocalDateTime lastHeartbeatTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
