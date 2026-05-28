package cn.ai.sales.module.agent.controller.admin.knowledge.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠知识库分页 Request VO")
@Data
public class AgentKnowledgeItemPageReqVO extends PageParam {

    @Schema(description = "知识库编号", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "关键词，匹配标题", example = "价格")
    private String keyword;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
