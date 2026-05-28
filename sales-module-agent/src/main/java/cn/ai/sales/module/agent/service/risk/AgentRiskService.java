package cn.ai.sales.module.agent.service.risk;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.risk.vo.AgentRiskPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;

import java.util.List;

public interface AgentRiskService {

    PageResult<AgentConversationDO> getRiskPage(AgentRiskPageReqVO pageReqVO);

    List<AgentMessageDO> getRiskMessages(Long conversationId);

    void takeover(Long conversationId);

    void close(Long conversationId);

}
