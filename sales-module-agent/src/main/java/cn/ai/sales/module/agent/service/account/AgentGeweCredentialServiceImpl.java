package cn.ai.sales.module.agent.service.account;

import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentGeweCredentialSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentGeweCredentialDO;
import cn.ai.sales.module.agent.dal.mysql.AgentGeweCredentialMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
@Slf4j
public class AgentGeweCredentialServiceImpl implements AgentGeweCredentialService {

    @Resource
    private AgentGeweCredentialMapper credentialMapper;
    @Resource
    private GeweMessageClient geweMessageClient;

    @Override
    public Long saveCredential(AgentGeweCredentialSaveReqVO saveReqVO) {
        AgentGeweCredentialDO existed = saveReqVO.getId() == null ? null : credentialMapper.selectById(saveReqVO.getId());
        AgentGeweCredentialDO credential = BeanUtils.toBean(saveReqVO, AgentGeweCredentialDO.class);
        if (existed == null) {
            credential.setCallbackToken(IdUtil.fastSimpleUUID());
            credential.setCallbackUrl(normalizeCallbackUrl(saveReqVO.getCallbackUrl(), credential.getCallbackToken()));
            if (credential.getStatus() == null) {
                credential.setStatus(AgentConstants.STATUS_ENABLE);
            }
            tryConfigureCallback(credential);
            credentialMapper.insert(credential);
            return credential.getId();
        }

        credential.setId(existed.getId());
        credential.setCallbackToken(existed.getCallbackToken());
        credential.setCallbackUrl(normalizeCallbackUrl(saveReqVO.getCallbackUrl(), existed.getCallbackToken()));
        if (StrUtil.isBlank(credential.getGeweToken())) {
            credential.setGeweToken(existed.getGeweToken());
        }
        tryConfigureCallback(credential);
        credentialMapper.updateById(credential);
        return credential.getId();
    }

    @Override
    public AgentGeweCredentialDO getCredential(Long id) {
        return id == null ? null : credentialMapper.selectById(id);
    }

    @Override
    public List<AgentGeweCredentialDO> getCredentialList() {
        return credentialMapper.selectListByOrder();
    }

    @Override
    public List<AgentGeweCredentialDO> getEnabledCredentialList() {
        return credentialMapper.selectEnabledList();
    }

    @Override
    public Long saveDefaultCredential(AgentGeweCredentialSaveReqVO saveReqVO) {
        if (saveReqVO.getId() == null) {
            AgentGeweCredentialDO existed = credentialMapper.selectFirst();
            if (existed != null) {
                saveReqVO.setId(existed.getId());
            }
        }
        return saveCredential(saveReqVO);
    }

    @Override
    public AgentGeweCredentialDO getDefaultCredential() {
        return credentialMapper.selectFirst();
    }

    private String normalizeCallbackUrl(String callbackUrl, String callbackToken) {
        if (StrUtil.isNotBlank(callbackUrl)) {
            return callbackUrl;
        }
        return "/api/v1/gewechat/callback?token=" + callbackToken;
    }

    private void tryConfigureCallback(AgentGeweCredentialDO credential) {
        if (StrUtil.isBlank(credential.getGeweToken()) || !StrUtil.startWithIgnoreCase(credential.getCallbackUrl(), "http")) {
            return;
        }
        try {
            if (geweMessageClient.setCallback(credential, credential.getCallbackUrl())) {
                credential.setCallbackConfiguredTime(LocalDateTime.now());
            }
        } catch (RuntimeException ex) {
            log.warn("[tryConfigureCallback][credentialId({}) callbackUrl({}) failed]",
                    credential.getId(), credential.getCallbackUrl(), ex);
        }
    }

}
