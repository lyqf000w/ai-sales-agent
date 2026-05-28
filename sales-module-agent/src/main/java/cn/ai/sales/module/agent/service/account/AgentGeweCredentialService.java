package cn.ai.sales.module.agent.service.account;

import cn.ai.sales.module.agent.controller.admin.account.vo.AgentGeweCredentialSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentGeweCredentialDO;
import jakarta.validation.Valid;

import java.util.List;

public interface AgentGeweCredentialService {

    Long saveCredential(@Valid AgentGeweCredentialSaveReqVO saveReqVO);

    AgentGeweCredentialDO getCredential(Long id);

    List<AgentGeweCredentialDO> getCredentialList();

    List<AgentGeweCredentialDO> getEnabledCredentialList();

    Long saveDefaultCredential(@Valid AgentGeweCredentialSaveReqVO saveReqVO);

    AgentGeweCredentialDO getDefaultCredential();

}
