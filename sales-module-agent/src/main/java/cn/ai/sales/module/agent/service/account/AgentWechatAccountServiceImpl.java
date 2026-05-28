package cn.ai.sales.module.agent.service.account;

import cn.hutool.core.util.IdUtil;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatAccountPageReqVO;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatAccountSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentGeweCredentialDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.mysql.AgentGeweCredentialMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.ai.sales.module.agent.service.reply.AgentReplyPolicyActivationService;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Map;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WECHAT_ACCOUNT_GEWE_APP_DUPLICATE;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WECHAT_ACCOUNT_NOT_EXISTS;

@Service
@Validated
public class AgentWechatAccountServiceImpl implements AgentWechatAccountService {

    @Resource
    private AgentWechatAccountMapper wechatAccountMapper;
    @Resource
    private AgentGeweCredentialMapper credentialMapper;
    @Resource
    private GeweMessageClient geweMessageClient;
    @Resource
    private AgentReplyPolicyActivationService replyPolicyActivationService;

    @Override
    public Long createWechatAccount(AgentWechatAccountSaveReqVO createReqVO) {
        validateGeweAppIdDuplicate(createReqVO.getGeweAppId(), null);

        AgentWechatAccountDO account = BeanUtils.toBean(createReqVO, AgentWechatAccountDO.class);
        String callbackToken = IdUtil.fastSimpleUUID();
        account.setCallbackToken(callbackToken);
        account.setCallbackUrl(resolveCredentialCallbackUrl(account.getGeweCredentialId(), callbackToken));
        account.setLoginStatus(AgentConstants.LOGIN_STATUS_UNKNOWN);
        fillReplyPolicyDefaults(account);
        wechatAccountMapper.insert(account);
        return account.getId();
    }

    @Override
    public void updateWechatAccount(AgentWechatAccountSaveReqVO updateReqVO) {
        AgentWechatAccountDO existedAccount = validateWechatAccountExists(updateReqVO.getId());
        validateGeweAppIdDuplicate(updateReqVO.getGeweAppId(), updateReqVO.getId());

        AgentWechatAccountDO updateObj = BeanUtils.toBean(updateReqVO, AgentWechatAccountDO.class);
        if (updateObj.getGeweCredentialId() == null) {
            updateObj.setGeweCredentialId(existedAccount.getGeweCredentialId());
        }
        if (StrUtil.isBlank(updateObj.getCallbackToken())) {
            updateObj.setCallbackToken(existedAccount.getCallbackToken());
        }
        if (StrUtil.isBlank(updateObj.getCallbackUrl())) {
            updateObj.setCallbackUrl(existedAccount.getCallbackUrl());
        }
        if (StrUtil.isBlank(updateObj.getCallbackSecret())) {
            updateObj.setCallbackSecret(existedAccount.getCallbackSecret());
        }
        if (StrUtil.isBlank(updateObj.getGeweApiBaseUrl())) {
            updateObj.setGeweApiBaseUrl(existedAccount.getGeweApiBaseUrl());
        }
        if (StrUtil.isBlank(updateObj.getGeweToken())) {
            updateObj.setGeweToken(existedAccount.getGeweToken());
        }
        if (StrUtil.isBlank(updateObj.getReplyMode())) {
            updateObj.setReplyMode(existedAccount.getReplyMode());
        }
        if (updateObj.getQuietSeconds() == null && updateObj.getQuietMinutes() == null) {
            updateObj.setQuietSeconds(existedAccount.getQuietSeconds());
            updateObj.setQuietMinutes(existedAccount.getQuietMinutes());
        }
        if (updateObj.getBusinessHours() == null) {
            updateObj.setBusinessHours(existedAccount.getBusinessHours());
        }
        fillReplyPolicyDefaults(updateObj);
        wechatAccountMapper.updateById(updateObj);
        if (AgentConstants.REPLY_MODE_AUTO_REPLY.equals(updateObj.getReplyMode())) {
            replyPolicyActivationService.activateAccountAutoReply(updateReqVO.getId());
        }
    }

    @Override
    public void deleteWechatAccount(Long id) {
        validateWechatAccountExists(id);
        wechatAccountMapper.deleteById(id);
    }

    @Override
    public AgentWechatAccountDO getWechatAccount(Long id) {
        AgentWechatAccountDO account = wechatAccountMapper.selectById(id);
        if (account != null) {
            syncLoginStatus(account);
            fillReplyPolicyDefaults(account);
        }
        return account;
    }

    @Override
    public PageResult<AgentWechatAccountDO> getWechatAccountPage(AgentWechatAccountPageReqVO pageReqVO) {
        PageResult<AgentWechatAccountDO> pageResult = wechatAccountMapper.selectPage(pageReqVO);
        pageResult.getList().forEach(this::fillReplyPolicyDefaults);
        return pageResult;
    }

    @Override
    public AgentWechatAccountDO getWechatAccountByCallbackToken(String callbackToken) {
        return wechatAccountMapper.selectByCallbackToken(callbackToken);
    }

    private AgentWechatAccountDO validateWechatAccountExists(Long id) {
        AgentWechatAccountDO account = wechatAccountMapper.selectById(id);
        if (account == null) {
            throw exception(WECHAT_ACCOUNT_NOT_EXISTS);
        }
        return account;
    }

    private void syncLoginStatus(AgentWechatAccountDO account) {
        Boolean online = geweMessageClient.checkOnline(account);
        if (online == null) {
            return;
        }
        int latestLoginStatus = online ? AgentConstants.LOGIN_STATUS_ONLINE : AgentConstants.LOGIN_STATUS_OFFLINE;
        LocalDateTime now = LocalDateTime.now();
        AgentWechatAccountDO update = new AgentWechatAccountDO();
        update.setId(account.getId());
        update.setLoginStatus(latestLoginStatus);
        update.setLastHeartbeatTime(now);
        wechatAccountMapper.updateById(update);
        account.setLoginStatus(latestLoginStatus);
        account.setLastHeartbeatTime(now);
    }

    private void validateGeweAppIdDuplicate(String geweAppId, Long id) {
        if (StrUtil.isBlank(geweAppId)) {
            return;
        }
        AgentWechatAccountDO account = wechatAccountMapper.selectByGeweAppId(geweAppId);
        if (account == null) {
            return;
        }
        if (id == null || !Objects.equals(account.getId(), id)) {
            throw exception(WECHAT_ACCOUNT_GEWE_APP_DUPLICATE);
        }
    }

    private void fillReplyPolicyDefaults(AgentWechatAccountDO account) {
        if (AgentConstants.REPLY_MODE_MANUAL_ONLY.equals(account.getReplyMode())) {
            account.setReplyMode(AgentConstants.REPLY_MODE_MANUAL_CONFIRM);
        }
        if (account.getReplyMode() == null) {
            account.setReplyMode(AgentConstants.REPLY_MODE_MANUAL_CONFIRM);
        }
        if (account.getQuietSeconds() == null) {
            account.setQuietSeconds(account.getQuietMinutes() == null
                    ? AgentConstants.DEFAULT_QUIET_SECONDS : account.getQuietMinutes() * 60);
        }
        if (account.getQuietSeconds() <= 0) {
            account.setQuietSeconds(AgentConstants.DEFAULT_QUIET_SECONDS);
        }
        if (account.getBusinessHours() == null
                || !account.getBusinessHours().containsKey("start")
                || !account.getBusinessHours().containsKey("end")) {
            account.setBusinessHours(Map.of("start", AgentConstants.DEFAULT_BUSINESS_HOURS_START,
                    "end", AgentConstants.DEFAULT_BUSINESS_HOURS_END));
        }
    }

    private String resolveCredentialCallbackUrl(Long credentialId, String fallbackToken) {
        if (credentialId != null) {
            AgentGeweCredentialDO credential = credentialMapper.selectById(credentialId);
            if (credential != null && StrUtil.isNotBlank(credential.getCallbackUrl())) {
                return credential.getCallbackUrl();
            }
            if (credential != null && StrUtil.isNotBlank(credential.getCallbackToken())) {
                return "/api/v1/gewechat/callback?token=" + credential.getCallbackToken();
            }
        }
        return "/api/v1/gewechat/callback?token=" + fallbackToken;
    }
}
