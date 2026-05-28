package cn.ai.sales.module.agent.controller.admin.contact.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "管理后台 - AI 销冠微信好友回复策略覆盖 Request VO")
@Data
public class AgentWechatContactUpdateReplyPolicyReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "好友回复模式覆盖，空表示继承微信号策略", example = "AUTO_REPLY")
    private String replyMode;

    @Schema(description = "历史兼容字段：好友自动回复最低置信度覆盖，空表示继承", example = "0.80")
    private BigDecimal confidenceThreshold;

    @Schema(description = "历史兼容字段：好友连续自动回复上限覆盖，空表示继承", example = "5")
    private Integer maxContinuousAutoReply;

    @Schema(description = "历史兼容字段：好友静默分钟数覆盖，空表示继承", example = "10")
    private Integer quietMinutes;

    @Schema(description = "好友静默秒数覆盖，空表示继承", example = "30")
    private Integer quietSeconds;

    @Schema(description = "好友营业时间覆盖，空表示继承")
    private Map<String, Object> businessHours;

}
