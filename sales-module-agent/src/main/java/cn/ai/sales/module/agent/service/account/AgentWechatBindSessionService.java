package cn.ai.sales.module.agent.service.account;

import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatBindSessionCreateReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatBindSessionDO;
import jakarta.validation.Valid;

public interface AgentWechatBindSessionService {

    Long createBindSession(@Valid AgentWechatBindSessionCreateReqVO createReqVO);

    AgentWechatBindSessionDO getBindSession(Long id);

    AgentWechatBindSessionDO checkBindSession(Long id);

}
