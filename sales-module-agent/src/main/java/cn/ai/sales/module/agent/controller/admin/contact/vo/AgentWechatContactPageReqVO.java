package cn.ai.sales.module.agent.controller.admin.contact.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠微信好友分页 Request VO")
@Data
public class AgentWechatContactPageReqVO extends PageParam {

    @Schema(description = "微信账号编号", example = "1")
    private Long wechatAccountId;

    @Schema(description = "微信好友编号", example = "1")
    private Long contactId;

    @Schema(description = "客户标签编号", example = "1")
    private Long tagId;

    @Schema(description = "关键词，匹配昵称", example = "张三")
    private String keyword;

    @Schema(description = "客户等级", example = "1")
    private Integer customerLevel;

    @Schema(description = "风险等级", example = "0")
    private Integer riskLevel;

    @Schema(description = "购买意愿", example = "HIGH")
    private String purchaseIntention;

    @Schema(description = "销售阶段", example = "QUOTE_NEGOTIATION")
    private String salesStage;

    @Schema(description = "客户情绪", example = "POSITIVE")
    private String customerSentiment;

    @Schema(description = "跟进优先级", example = "URGENT")
    private String followUpPriority;

    @Schema(description = "会话好友队列类型", example = "PENDING_REVIEW")
    private String queueType;

}
