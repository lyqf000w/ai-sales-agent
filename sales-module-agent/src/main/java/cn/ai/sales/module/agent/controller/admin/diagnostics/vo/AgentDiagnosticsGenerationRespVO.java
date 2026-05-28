package cn.ai.sales.module.agent.controller.admin.diagnostics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Admin - recent reply generation diagnostics")
@Data
public class AgentDiagnosticsGenerationRespVO {

    private Long decisionId;
    private Long conversationId;
    private String generationSource;
    private String llmProvider;
    private String llmModel;
    private String reviewStatus;
    private LocalDateTime createTime;

}
