package cn.ai.sales.module.agent.controller.admin.knowledge.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - AI 销冠知识库分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AgentKnowledgeBasePageReqVO extends PageParam {

    @Schema(description = "关键词", example = "商品库")
    private String keyword;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
