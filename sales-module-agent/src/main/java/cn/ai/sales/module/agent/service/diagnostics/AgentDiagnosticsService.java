package cn.ai.sales.module.agent.service.diagnostics;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentDiagnosticsSummaryRespVO;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentWebhookEventPageReqVO;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentWebhookEventRespVO;

public interface AgentDiagnosticsService {

    AgentDiagnosticsSummaryRespVO getSummary();

    PageResult<AgentWebhookEventRespVO> getWebhookEventPage(AgentWebhookEventPageReqVO pageReqVO);

    AgentWebhookEventRespVO getWebhookEvent(Long id);

}
