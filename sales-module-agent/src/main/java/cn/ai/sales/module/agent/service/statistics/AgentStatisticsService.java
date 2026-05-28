package cn.ai.sales.module.agent.service.statistics;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsMessagePageReqVO;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsMessageRespVO;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsSummaryRespVO;

public interface AgentStatisticsService {

    AgentStatisticsSummaryRespVO getSummary();

    PageResult<AgentStatisticsMessageRespVO> getMessagePage(AgentStatisticsMessagePageReqVO pageReqVO);

}
