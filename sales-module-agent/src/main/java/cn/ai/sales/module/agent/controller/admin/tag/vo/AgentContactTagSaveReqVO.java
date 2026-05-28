package cn.ai.sales.module.agent.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠客户标签新增/修改 Request VO")
@Data
public class AgentContactTagSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "高意向")
    @NotEmpty(message = "标签名称不能为空")
    private String name;

    @Schema(description = "颜色", example = "#409EFF")
    private String color;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

}
