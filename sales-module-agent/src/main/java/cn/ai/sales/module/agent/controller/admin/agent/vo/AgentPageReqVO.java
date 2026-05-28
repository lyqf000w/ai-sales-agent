package cn.ai.sales.module.agent.controller.admin.agent.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠 Agent 分页 Request VO")
@Data
public class AgentPageReqVO extends PageParam {

    @Schema(description = "关键词，匹配名称", example = "销冠")
    private String keyword;

    @Schema(description = "回复模式", example = "MANUAL_CONFIRM")
    private String replyMode;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
