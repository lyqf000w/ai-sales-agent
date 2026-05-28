package cn.ai.sales.module.agent.controller.admin.sensitiverule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 销冠人工升级规则 Response VO")
@Data
public class AgentSensitiveRuleRespVO {

    @Schema(description = "编号")
    private Long id;
    @Schema(description = "规则名称")
    private String name;
    @Schema(description = "Agent 编号")
    private Long agentId;
    @Schema(description = "接入应用")
    private String routeApp;
    @Schema(description = "触发类型")
    private String triggerType;
    @Schema(description = "历史兼容字段：匹配方式")
    private Integer matchType;
    @Schema(description = "匹配内容")
    private String pattern;
    @Schema(description = "动作")
    private Integer action;
    @Schema(description = "风险等级")
    private Integer riskLevel;
    @Schema(description = "排序")
    private Integer sort;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
