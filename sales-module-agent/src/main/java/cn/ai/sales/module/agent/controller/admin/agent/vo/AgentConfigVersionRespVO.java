package cn.ai.sales.module.agent.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - AI 销冠 Agent 配置版本 Response VO")
@Data
public class AgentConfigVersionRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;
    @Schema(description = "Agent 编号", example = "1")
    private Long agentId;
    @Schema(description = "版本号", example = "3")
    private Integer version;
    @Schema(description = "配置快照")
    private Map<String, Object> configSnapshot;
    @Schema(description = "变更说明")
    private String changeSummary;
    @Schema(description = "发布人编号")
    private Long publishUserId;
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
