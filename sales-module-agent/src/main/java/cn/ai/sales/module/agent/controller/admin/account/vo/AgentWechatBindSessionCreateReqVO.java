package cn.ai.sales.module.agent.controller.admin.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠微信扫码绑定会话创建 Request VO")
@Data
public class AgentWechatBindSessionCreateReqVO {

    @Schema(description = "GeWe 凭证编号；为空使用租户默认可用凭证", example = "1")
    private Long credentialId;

    @Schema(description = "绑定 Agent 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "绑定 Agent 不能为空")
    private Long agentId;

    @Schema(description = "负责人用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "负责人不能为空")
    private Long ownerUserId;

}
