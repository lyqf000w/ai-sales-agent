package cn.ai.sales.module.agent.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 销冠运营统计维度 Response VO")
@Data
public class AgentStatisticsDimensionRespVO {

    @Schema(description = "维度编码", example = "HIGH")
    private String code;

    @Schema(description = "数量", example = "12")
    private Long count;

}
