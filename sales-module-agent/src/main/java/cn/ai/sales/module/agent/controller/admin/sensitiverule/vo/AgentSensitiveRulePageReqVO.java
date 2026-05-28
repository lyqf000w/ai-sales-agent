package cn.ai.sales.module.agent.controller.admin.sensitiverule.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠人工升级规则分页 Request VO")
@Data
public class AgentSensitiveRulePageReqVO extends PageParam {

    @Schema(description = "关键词，匹配名称", example = "退款")
    private String keyword;

    @Schema(description = "历史兼容字段：匹配方式", example = "1")
    private Integer matchType;

    @Schema(description = "触发类型", example = "INTENT")
    private String triggerType;

    @Schema(description = "动作", example = "1")
    private Integer action;

    @Schema(description = "Agent 编号", example = "1")
    private Long agentId;

    @Schema(description = "接入应用", example = "GEWE")
    private String routeApp;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
