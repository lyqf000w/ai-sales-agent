package cn.ai.sales.module.agent.service.conversation;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactPageReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationContactRespVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationContactSettingsRespVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationContactSettingsSaveReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationMessageReviewReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationPageReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationRestoreOriginalPolicyReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationSendMessageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;

import java.util.List;

public interface AgentConversationService {

    PageResult<AgentConversationDO> getConversationPage(AgentConversationPageReqVO pageReqVO);

    PageResult<AgentConversationContactRespVO> getConversationContactPage(AgentWechatContactPageReqVO pageReqVO);

    List<AgentMessageDO> getConversationMessages(Long conversationId);

    Long sendMessage(AgentConversationSendMessageReqVO sendReqVO);

    void restoreOriginalPolicy(AgentConversationRestoreOriginalPolicyReqVO restoreReqVO);

    boolean acknowledgePendingView(AgentConversationRestoreOriginalPolicyReqVO reqVO);

    AgentConversationContactSettingsRespVO saveContactSettings(AgentConversationContactSettingsSaveReqVO saveReqVO);

    void approveMessage(AgentConversationMessageReviewReqVO reviewReqVO);

    void rejectMessage(AgentConversationMessageReviewReqVO reviewReqVO);

}
