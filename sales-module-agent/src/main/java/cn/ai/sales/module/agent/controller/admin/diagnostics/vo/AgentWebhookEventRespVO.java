package cn.ai.sales.module.agent.controller.admin.diagnostics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Admin - GeWe webhook event response")
@Data
public class AgentWebhookEventRespVO {

    private Long id;
    private Long wechatAccountId;
    private String wechatAccountName;
    private String wechatId;
    private String contactWxid;
    private String contactDisplayName;
    private String groupDisplayName;
    private String groupMemberDisplayName;
    private String eventSummary;
    private String eventId;
    private String eventType;
    private String eventTypeName;
    private Boolean signatureValid;
    private Integer processStatus;
    private String errorMessage;
    private Map<String, Object> rawPayload;
    private LocalDateTime createTime;

}
