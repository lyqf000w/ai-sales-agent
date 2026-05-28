package cn.ai.sales.module.agent.controller.admin.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠 GeWe 凭证保存 Request VO")
@Data
public class AgentGeweCredentialSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "配置名称不能为空")
    private String name;

    @Schema(description = "GeWe API 地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "http://api.geweapi.com")
    @NotEmpty(message = "GeWe API 地址不能为空")
    private String geweApiBaseUrl;

    @Schema(description = "GeWe Token")
    private String geweToken;

    @Schema(description = "回调签名密钥")
    private String callbackSecret;

    @Schema(description = "公网回调地址，留空则生成本地路径")
    private String callbackUrl;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

}
