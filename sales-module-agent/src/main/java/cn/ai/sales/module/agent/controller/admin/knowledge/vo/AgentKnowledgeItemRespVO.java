package cn.ai.sales.module.agent.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 销冠知识库 Response VO")
@Data
public class AgentKnowledgeItemRespVO {

    @Schema(description = "编号")
    private Long id;
    @Schema(description = "知识库编号")
    private Long knowledgeBaseId;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "商品名称")
    private String productName;
    @Schema(description = "分类")
    private String category;
    @Schema(description = "关键词")
    private String keywords;
    @Schema(description = "客户问题")
    private String question;
    @Schema(description = "回复答案")
    private String answer;
    @Schema(description = "向量化状态")
    private String embeddingStatus;
    @Schema(description = "排序")
    private Integer sort;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
