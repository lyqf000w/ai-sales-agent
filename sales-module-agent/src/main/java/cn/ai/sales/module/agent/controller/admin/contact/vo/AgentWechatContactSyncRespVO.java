package cn.ai.sales.module.agent.controller.admin.contact.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Admin - WeChat contact sync response")
@Data
public class AgentWechatContactSyncRespVO {

    @Schema(description = "Synced WeChat account count", example = "1")
    private Integer accountCount;
    @Schema(description = "Contacts returned by GeWe", example = "120")
    private Integer fetchedCount;
    @Schema(description = "Inserted contact count", example = "20")
    private Integer createdCount;
    @Schema(description = "Updated contact count", example = "100")
    private Integer updatedCount;
    @Schema(description = "Skipped contact count", example = "3")
    private Integer skippedCount;

    public void add(AgentWechatContactSyncRespVO other) {
        if (other == null) {
            return;
        }
        accountCount = sum(accountCount, other.accountCount);
        fetchedCount = sum(fetchedCount, other.fetchedCount);
        createdCount = sum(createdCount, other.createdCount);
        updatedCount = sum(updatedCount, other.updatedCount);
        skippedCount = sum(skippedCount, other.skippedCount);
    }

    private Integer sum(Integer left, Integer right) {
        return (left == null ? 0 : left) + (right == null ? 0 : right);
    }

}
