package cn.ai.sales.module.agent.service.contact;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactPageReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactSyncRespVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateLevelReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateOwnerReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateReplyPolicyReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateSalesInsightReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import jakarta.validation.Valid;

public interface AgentWechatContactService {

    AgentWechatContactDO getWechatContact(Long id);

    PageResult<AgentWechatContactDO> getWechatContactPage(AgentWechatContactPageReqVO pageReqVO);

    AgentWechatContactSyncRespVO syncWechatContacts(Long wechatAccountId);

    void updateWechatContactLevel(@Valid AgentWechatContactUpdateLevelReqVO updateReqVO);

    void updateWechatContactOwner(@Valid AgentWechatContactUpdateOwnerReqVO updateReqVO);

    void updateWechatContactReplyPolicy(@Valid AgentWechatContactUpdateReplyPolicyReqVO updateReqVO);

    void updateWechatContactSalesInsight(@Valid AgentWechatContactUpdateSalesInsightReqVO updateReqVO);

}
