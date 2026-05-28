package cn.ai.sales.module.agent.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 销冠知识库 Response VO")
@Data
public class AgentKnowledgeBaseRespVO {

    @Schema(description = "编号")
    private Long id;
    @Schema(description = "知识库名称")
    private String name;
    @Schema(description = "说明")
    private String description;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
