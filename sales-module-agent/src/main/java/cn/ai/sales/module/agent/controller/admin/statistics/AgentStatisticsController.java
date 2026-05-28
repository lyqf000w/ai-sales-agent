package cn.ai.sales.module.agent.controller.admin.statistics;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsMessagePageReqVO;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsMessageRespVO;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsSummaryRespVO;
import cn.ai.sales.module.agent.service.statistics.AgentStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 销冠运营统计")
@RestController
@RequestMapping("/agent/statistics")
@Validated
public class AgentStatisticsController {

    @Resource
    private AgentStatisticsService statisticsService;

    @GetMapping("/summary")
    @Operation(summary = "获得运营统计概览")
    @PreAuthorize("@ss.hasPermission('agent:statistics:query')")
    public CommonResult<AgentStatisticsSummaryRespVO> getSummary() {
        return success(statisticsService.getSummary());
    }

    @GetMapping("/messages")
    @Operation(summary = "Get statistics message details")
    @PreAuthorize("@ss.hasPermission('agent:statistics:query')")
    public CommonResult<PageResult<AgentStatisticsMessageRespVO>> getMessagePage(
            @Valid AgentStatisticsMessagePageReqVO pageReqVO) {
        return success(statisticsService.getMessagePage(pageReqVO));
    }

}
