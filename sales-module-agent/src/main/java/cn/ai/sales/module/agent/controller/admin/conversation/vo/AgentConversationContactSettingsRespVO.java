package cn.ai.sales.module.agent.controller.admin.conversation.vo;

import cn.ai.sales.module.agent.controller.admin.replypolicy.vo.AgentReplyPolicyRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "Admin - conversation workbench contact settings response")
@Data
public class AgentConversationContactSettingsRespVO {

    @Schema(description = "Latest contact row for the workbench")
    private AgentConversationContactRespVO contact;

    @Schema(description = "Latest effective reply policy")
    private AgentReplyPolicyRespVO policy;

    @Schema(description = "Latest contact tag ids")
    private List<Long> tagIds;

}
