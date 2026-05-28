package cn.ai.sales.module.agent.controller.admin.contact.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠微信好友负责人更新 Request VO")
@Data
public class AgentWechatContactUpdateOwnerReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "负责人用户编号", example = "1")
    private Long ownerUserId;

}
