package cn.ai.sales.module.agent.controller.admin.conversation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "Admin - conversation workbench contact settings save request")
@Data
public class AgentConversationContactSettingsSaveReqVO {

    @Schema(description = "Conversation id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "Conversation id cannot be empty")
    private Long conversationId;

    @Schema(description = "Contact id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "Contact id cannot be empty")
    private Long contactId;

    @Schema(description = "Customer level", example = "1")
    private Integer customerLevel;

    @Schema(description = "Contact reply mode override. Null means inherit account policy", example = "AUTO_REPLY")
    private String replyMode;

    @Schema(description = "Contact quiet seconds override. Null means inherit account policy", example = "90")
    private Integer quietSeconds;

    @Schema(description = "Contact business hours override. Null means inherit account policy")
    private Map<String, Object> businessHours;

    @Schema(description = "Purchase intention", example = "MEDIUM")
    private String purchaseIntention;

    @Schema(description = "Sales stage", example = "NEW_LEAD")
    private String salesStage;

    @Schema(description = "Customer sentiment", example = "NEUTRAL")
    private String customerSentiment;

    @Schema(description = "Follow-up priority", example = "NORMAL")
    private String followUpPriority;

    @Schema(description = "Contact tag ids")
    private List<Long> tagIds;

}
