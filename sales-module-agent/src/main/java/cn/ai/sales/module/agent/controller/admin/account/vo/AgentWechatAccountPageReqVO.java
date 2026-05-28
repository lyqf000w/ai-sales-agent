package cn.ai.sales.module.agent.controller.admin.account.vo;

import cn.ai.sales.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.ai.sales.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - AI 销冠微信账号分页 Request VO")
@Data
public class AgentWechatAccountPageReqVO extends PageParam {

    @Schema(description = "关键词，匹配昵称", example = "销售微信")
    private String keyword;

    @Schema(description = "账号状态", example = "0")
    private Integer status;

    @Schema(description = "登录状态", example = "1")
    private Integer loginStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
