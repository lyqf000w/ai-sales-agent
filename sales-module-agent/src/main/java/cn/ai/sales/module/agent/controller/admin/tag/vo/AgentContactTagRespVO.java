package cn.ai.sales.module.agent.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 销冠客户标签 Response VO")
@Data
public class AgentContactTagRespVO {

    @Schema(description = "编号")
    private Long id;
    @Schema(description = "标签名称")
    private String name;
    @Schema(description = "颜色")
    private String color;
    @Schema(description = "说明")
    private String description;
    @Schema(description = "排序")
    private Integer sort;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
