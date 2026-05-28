package cn.ai.sales.module.agent.controller.admin.conversation.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠会话分页 Request VO")
@Data
public class AgentConversationPageReqVO extends PageParam {

    @Schema(description = "微信账号编号", example = "1")
    private Long wechatAccountId;

    @Schema(description = "微信好友编号", example = "1")
    private Long contactId;

    @Schema(description = "会话状态", example = "0")
    private Integer status;

    @Schema(description = "风险等级", example = "0")
    private Integer riskLevel;

    @Schema(description = "会话队列：PENDING_REVIEW 待确认，TAKEOVER 人工接管，FOCUS 重点跟进，URGENT 紧急跟进，RISK 历史兼容风险", example = "TAKEOVER")
    private String queueType;

}
