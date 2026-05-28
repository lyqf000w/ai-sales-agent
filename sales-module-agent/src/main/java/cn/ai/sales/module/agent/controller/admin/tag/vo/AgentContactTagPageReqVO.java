package cn.ai.sales.module.agent.controller.admin.tag.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠客户标签分页 Request VO")
@Data
public class AgentContactTagPageReqVO extends PageParam {

    @Schema(description = "关键词，匹配名称", example = "高意向")
    private String keyword;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
