package cn.ai.sales.module.agent.controller.admin.diagnostics;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentDiagnosticsSummaryRespVO;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentWebhookEventPageReqVO;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentWebhookEventRespVO;
import cn.ai.sales.module.agent.service.diagnostics.AgentDiagnosticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - personal sales assistant diagnostics")
@RestController
@RequestMapping("/agent/diagnostics")
@Validated
public class AgentDiagnosticsController {

    @Resource
    private AgentDiagnosticsService diagnosticsService;

    @GetMapping("/summary")
    @Operation(summary = "Get diagnostics summary")
    @PreAuthorize("@ss.hasPermission('agent:diagnostics:query')")
    public CommonResult<AgentDiagnosticsSummaryRespVO> getSummary() {
        return success(diagnosticsService.getSummary());
    }

    @GetMapping("/webhook-event/page")
    @Operation(summary = "Get GeWe webhook event page")
    @PreAuthorize("@ss.hasPermission('agent:diagnostics:query')")
    public CommonResult<PageResult<AgentWebhookEventRespVO>> getWebhookEventPage(
            @Valid AgentWebhookEventPageReqVO pageReqVO) {
        return success(diagnosticsService.getWebhookEventPage(pageReqVO));
    }

    @GetMapping("/webhook-event/get")
    @Operation(summary = "Get GeWe webhook event detail")
    @Parameter(name = "id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:diagnostics:query')")
    public CommonResult<AgentWebhookEventRespVO> getWebhookEvent(@RequestParam("id") Long id) {
        return success(diagnosticsService.getWebhookEvent(id));
    }

}
