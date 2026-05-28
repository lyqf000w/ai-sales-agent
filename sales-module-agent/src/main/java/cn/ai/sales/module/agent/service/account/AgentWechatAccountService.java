package cn.ai.sales.module.agent.service.account;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatAccountPageReqVO;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatAccountSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import jakarta.validation.Valid;

public interface AgentWechatAccountService {

    Long createWechatAccount(@Valid AgentWechatAccountSaveReqVO createReqVO);

    void updateWechatAccount(@Valid AgentWechatAccountSaveReqVO updateReqVO);

    void deleteWechatAccount(Long id);

    AgentWechatAccountDO getWechatAccount(Long id);

    PageResult<AgentWechatAccountDO> getWechatAccountPage(AgentWechatAccountPageReqVO pageReqVO);

    AgentWechatAccountDO getWechatAccountByCallbackToken(String callbackToken);

}
