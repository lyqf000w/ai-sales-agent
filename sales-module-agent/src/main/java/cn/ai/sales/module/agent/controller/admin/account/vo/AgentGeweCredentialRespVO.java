package cn.ai.sales.module.agent.controller.admin.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 销冠 GeWe 凭证 Response VO")
@Data
public class AgentGeweCredentialRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;
    @Schema(description = "配置名称")
    private String name;
    @Schema(description = "GeWe API 地址")
    private String geweApiBaseUrl;
    @Schema(description = "回调令牌")
    private String callbackToken;
    @Schema(description = "回调地址")
    private String callbackUrl;
    @Schema(description = "是否已配置 GeWe Token")
    private Boolean geweTokenConfigured;
    @Schema(description = "回调配置时间")
    private LocalDateTime callbackConfiguredTime;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
