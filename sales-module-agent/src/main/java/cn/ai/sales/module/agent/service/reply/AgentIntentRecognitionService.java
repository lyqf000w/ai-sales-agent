package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;

public interface AgentIntentRecognitionService {

    AgentIntentAnalysis recognize(AgentDO agent, AgentWechatContactDO contact, AgentMessageDO inboundMessage);

}
