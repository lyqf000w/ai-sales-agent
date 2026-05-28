package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;

import java.time.LocalDateTime;

public interface AgentAutoReplyService {

    void handleInboundMessage(AgentWechatAccountDO account, AgentWechatContactDO contact,
                              AgentConversationDO conversation, AgentMessageDO inboundMessage);

    void processDuePendingReplies(LocalDateTime now);

}
