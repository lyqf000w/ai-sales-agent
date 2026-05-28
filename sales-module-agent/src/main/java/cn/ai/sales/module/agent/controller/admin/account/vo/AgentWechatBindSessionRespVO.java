package cn.ai.sales.module.agent.controller.admin.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 销冠微信扫码绑定会话 Response VO")
@Data
public class AgentWechatBindSessionRespVO {

    @Schema(description = "编号")
    private Long id;
    @Schema(description = "GeWe 凭证编号")
    private Long credentialId;
    @Schema(description = "绑定 Agent 编号")
    private Long agentId;
    @Schema(description = "负责人用户编号")
    private Long ownerUserId;
    @Schema(description = "GeWe AppId")
    private String appId;
    @Schema(description = "登录 UUID")
    private String uuid;
    @Schema(description = "二维码链接")
    private String qrData;
    @Schema(description = "二维码图片 Base64")
    private String qrImgBase64;
    @Schema(description = "验证链接")
    private String verifyUrl;
    @Schema(description = "扫码昵称")
    private String nickName;
    @Schema(description = "扫码头像")
    private String avatar;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "过期时间")
    private LocalDateTime expiresAt;
    @Schema(description = "绑定微信账号编号")
    private Long bindAccountId;
    @Schema(description = "错误信息")
    private String errorMessage;

}
