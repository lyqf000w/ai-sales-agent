package cn.ai.sales.module.agent.controller.admin.statistics.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Admin - AI sales statistics message page request")
@Data
public class AgentStatisticsMessagePageReqVO extends PageParam {

    @Schema(description = "Message scope: TODAY_MESSAGES or TODAY_AUTO_REPLY", example = "TODAY_MESSAGES")
    private String scope;

}
