package cn.ai.sales.module.agent.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠知识库新增/修改 Request VO")
@Data
public class AgentKnowledgeItemSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "知识库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "知识库不能为空")
    private Long knowledgeBaseId;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "产品报价")
    @NotEmpty(message = "标题不能为空")
    private String title;

    @Schema(description = "商品名称", example = "AI 销冠企业版")
    private String productName;

    @Schema(description = "分类", example = "报价")
    private String category;

    @Schema(description = "关键词，逗号分隔", example = "报价,价格,费用")
    private String keywords;

    @Schema(description = "客户问题")
    private String question;

    @Schema(description = "回复答案", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "回复答案不能为空")
    private String answer;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

}
