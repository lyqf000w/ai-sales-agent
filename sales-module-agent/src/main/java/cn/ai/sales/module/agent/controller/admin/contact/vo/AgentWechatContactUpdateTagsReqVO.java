package cn.ai.sales.module.agent.controller.admin.contact.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - AI 销冠微信好友标签更新 Request VO")
@Data
public class AgentWechatContactUpdateTagsReqVO {

    @Schema(description = "微信好友编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "微信好友编号不能为空")
    private Long contactId;

    @Schema(description = "标签编号列表")
    private List<Long> tagIds;

}
