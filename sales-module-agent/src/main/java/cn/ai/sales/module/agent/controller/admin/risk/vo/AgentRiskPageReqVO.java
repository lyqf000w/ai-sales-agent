package cn.ai.sales.module.agent.controller.admin.risk.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠风险会话分页 Request VO")
@Data
public class AgentRiskPageReqVO extends PageParam {

    @Schema(description = "微信账号编号", example = "1")
    private Long wechatAccountId;

    @Schema(description = "微信好友编号", example = "1")
    private Long contactId;

    @Schema(description = "会话状态", example = "2")
    private Integer status;

    @Schema(description = "风险等级", example = "2")
    private Integer riskLevel;

}
