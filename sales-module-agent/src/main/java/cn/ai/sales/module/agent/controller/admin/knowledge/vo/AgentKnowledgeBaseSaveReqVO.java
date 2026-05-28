package cn.ai.sales.module.agent.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠知识库新增/修改 Request VO")
@Data
public class AgentKnowledgeBaseSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "知识库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "默认商品库")
    @NotEmpty(message = "知识库名称不能为空")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

}
