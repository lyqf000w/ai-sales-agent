package cn.ai.sales.module.agent.controller.admin.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "管理后台 - AI 销冠微信账号新增/修改 Request VO")
@Data
public class AgentWechatAccountSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "GeWe 凭证编号", example = "1")
    private Long geweCredentialId;

    @Schema(description = "绑定 Agent 编号", example = "1")
    @NotNull(message = "绑定 Agent 不能为空")
    private Long agentId;

    @Schema(description = "覆盖知识库编号，空表示使用 Agent 默认知识库", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "负责人用户编号", example = "1")
    @NotNull(message = "负责人不能为空")
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

    @Schema(description = "回调签名密钥")
    private String callbackSecret;

    @Schema(description = "Gewe API 地址", example = "http://api.geweapi.com")
    private String geweApiBaseUrl;

    @Schema(description = "Gewe API Token")
    private String geweToken;

    @Schema(description = "默认回复模式", example = "MANUAL_CONFIRM")
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

    @Schema(description = "账号状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "账号状态不能为空")
    private Integer status;

}
