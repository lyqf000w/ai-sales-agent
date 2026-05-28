package cn.ai.sales.module.agent.controller.admin.contact.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠微信好友等级更新 Request VO")
@Data
public class AgentWechatContactUpdateLevelReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "客户等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "客户等级不能为空")
    private Integer customerLevel;

}
