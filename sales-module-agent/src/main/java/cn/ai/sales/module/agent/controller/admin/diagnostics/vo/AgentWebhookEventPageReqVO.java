package cn.ai.sales.module.agent.controller.admin.diagnostics.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.ai.sales.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "Admin - GeWe webhook event page request")
@Data
public class AgentWebhookEventPageReqVO extends PageParam {

    @Schema(description = "WeChat account id", example = "1")
    private Long wechatAccountId;

    @Schema(description = "GeWe event id keyword")
    private String eventId;

    @Schema(description = "GeWe event type")
    private String eventType;

    @Schema(description = "Process status: 0 new, 1 processed, 2 duplicate, 3 failed")
    private Integer processStatus;

    @Schema(description = "Create time range")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
