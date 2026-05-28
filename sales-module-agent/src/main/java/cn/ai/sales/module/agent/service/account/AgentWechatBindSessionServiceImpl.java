package cn.ai.sales.module.agent.service.account;

import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatBindSessionCreateReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentGeweCredentialDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatBindSessionDO;
import cn.ai.sales.module.agent.dal.mysql.AgentGeweCredentialMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatBindSessionMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.gewe.GeweLoginCheckResult;
import cn.ai.sales.module.agent.service.gewe.GeweLoginQrCodeResult;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.ai.sales.module.agent.service.gewe.GeweProfileResult;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.GEWE_CREDENTIAL_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.GEWE_CREDENTIAL_TOKEN_MISSING;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WECHAT_BIND_SESSION_EXPIRED;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WECHAT_BIND_SESSION_FAILED;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WECHAT_BIND_SESSION_NOT_EXISTS;

@Service
@Validated
@Slf4j
public class AgentWechatBindSessionServiceImpl implements AgentWechatBindSessionService {

    private static final int QR_EXPIRE_SECONDS = 150;

    @Resource
    private AgentGeweCredentialMapper credentialMapper;
    @Resource
    private AgentWechatBindSessionMapper bindSessionMapper;
    @Resource
    private AgentWechatAccountMapper accountMapper;
    @Resource
    private GeweMessageClient geweMessageClient;

    @Override
    public Long createBindSession(AgentWechatBindSessionCreateReqVO createReqVO) {
        AgentGeweCredentialDO credential = getCredential(createReqVO.getCredentialId());
        if (StrUtil.isBlank(credential.getGeweToken())) {
            throw exception(GEWE_CREDENTIAL_TOKEN_MISSING);
        }
        GeweLoginQrCodeResult qrCode = geweMessageClient.getLoginQrCode(credential);

        AgentWechatBindSessionDO session = new AgentWechatBindSessionDO();
        session.setCredentialId(credential.getId());
        session.setAgentId(createReqVO.getAgentId());
        session.setOwnerUserId(createReqVO.getOwnerUserId());
        session.setAppId(qrCode.appId());
        session.setUuid(qrCode.uuid());
        session.setQrData(qrCode.qrData());
        session.setQrImgBase64(qrCode.qrImgBase64());
        session.setStatus(AgentConstants.BIND_SESSION_WAIT_SCAN);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(QR_EXPIRE_SECONDS));
        session.setRawResponse(qrCode.rawResponse());
        bindSessionMapper.insert(session);
        return session.getId();
    }

    @Override
    public AgentWechatBindSessionDO getBindSession(Long id) {
        return validateBindSessionExists(id);
    }

    @Override
    public AgentWechatBindSessionDO checkBindSession(Long id) {
        AgentWechatBindSessionDO session = validateBindSessionExists(id);
        if (Objects.equals(session.getStatus(), AgentConstants.BIND_SESSION_BOUND)
                || Objects.equals(session.getStatus(), AgentConstants.BIND_SESSION_FAILED)) {
            return session;
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            updateSessionStatus(session, AgentConstants.BIND_SESSION_EXPIRED, null);
            throw exception(WECHAT_BIND_SESSION_EXPIRED);
        }

        AgentGeweCredentialDO credential = validateCredentialExists(session.getCredentialId());
        GeweLoginCheckResult loginCheck;
        try {
            loginCheck = geweMessageClient.checkLogin(credential, session.getAppId(), session.getUuid());
        } catch (RuntimeException ex) {
            String errorMessage = "GeWe 登录状态检查失败：" + StrUtil.maxLength(ex.getMessage(), 220);
            log.warn("[checkBindSession][sessionId({}) appId({}) failed]", session.getId(), session.getAppId(), ex);
            updateSessionStatus(session, session.getStatus(), errorMessage);
            return session;
        }
        if (!loginCheck.success()) {
            AgentWechatBindSessionDO update = new AgentWechatBindSessionDO();
            update.setId(session.getId());
            update.setStatus(loginCheck.waitConfirm()
                    ? AgentConstants.BIND_SESSION_WAIT_CONFIRM : AgentConstants.BIND_SESSION_WAIT_SCAN);
            update.setVerifyUrl(loginCheck.verifyUrl());
            update.setNickName(loginCheck.nickName());
            update.setAvatar(loginCheck.avatar());
            update.setRawResponse(loginCheck.rawResponse());
            bindSessionMapper.updateById(update);
            session.setStatus(update.getStatus());
            session.setVerifyUrl(update.getVerifyUrl());
            session.setNickName(update.getNickName());
            session.setAvatar(update.getAvatar());
            session.setRawResponse(update.getRawResponse());
            return session;
        }

        try {
            return bindAccount(credential, session);
        } catch (RuntimeException ex) {
            updateSessionStatus(session, AgentConstants.BIND_SESSION_FAILED, StrUtil.maxLength(ex.getMessage(), 512));
            throw exception(WECHAT_BIND_SESSION_FAILED);
        }
    }

    private AgentWechatBindSessionDO bindAccount(AgentGeweCredentialDO credential, AgentWechatBindSessionDO session) {
        GeweProfileResult profile = geweMessageClient.getProfile(credential, session.getAppId());
        AgentWechatAccountDO account = accountMapper.selectByCredentialAndAppId(credential.getId(), session.getAppId());
        if (account == null && StrUtil.isNotBlank(profile.wxid())) {
            account = accountMapper.selectByWxid(profile.wxid());
        }
        if (account == null) {
            account = buildAccount(credential, session, profile, null);
            accountMapper.insert(account);
        } else {
            AgentWechatAccountDO update = buildAccount(credential, session, profile, account);
            update.setId(account.getId());
            accountMapper.updateById(update);
            account.setId(update.getId());
        }

        AgentWechatBindSessionDO update = new AgentWechatBindSessionDO();
        update.setId(session.getId());
        update.setStatus(AgentConstants.BIND_SESSION_BOUND);
        update.setBindAccountId(account.getId());
        update.setNickName(profile.nickName());
        update.setAvatar(profile.avatar());
        update.setRawResponse(profile.rawResponse());
        bindSessionMapper.updateById(update);

        session.setStatus(update.getStatus());
        session.setBindAccountId(update.getBindAccountId());
        session.setNickName(update.getNickName());
        session.setAvatar(update.getAvatar());
        session.setRawResponse(update.getRawResponse());
        return session;
    }

    private AgentWechatAccountDO buildAccount(AgentGeweCredentialDO credential, AgentWechatBindSessionDO session,
                                             GeweProfileResult profile, AgentWechatAccountDO existedAccount) {
        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setGeweCredentialId(credential.getId());
        account.setAgentId(session.getAgentId());
        account.setOwnerUserId(session.getOwnerUserId());
        account.setGeweAppId(session.getAppId());
        account.setGeweAccountId(session.getAppId());
        account.setWechatId(profile.wxid());
        account.setNickname(StrUtil.blankToDefault(profile.nickName(), session.getNickName()));
        account.setAvatar(StrUtil.blankToDefault(profile.avatar(), session.getAvatar()));
        account.setMobile(profile.mobile());
        String callbackToken = existedAccount == null || StrUtil.isBlank(existedAccount.getCallbackToken())
                ? IdUtil.fastSimpleUUID() : existedAccount.getCallbackToken();
        account.setCallbackToken(callbackToken);
        account.setCallbackSecret(existedAccount == null ? null : existedAccount.getCallbackSecret());
        account.setCallbackUrl(resolveCredentialCallbackUrl(credential.getId(), callbackToken));
        account.setReplyMode(AgentConstants.REPLY_MODE_MANUAL_CONFIRM);
        account.setQuietSeconds(AgentConstants.DEFAULT_QUIET_SECONDS);
        account.setBusinessHours(Map.of("start", AgentConstants.DEFAULT_BUSINESS_HOURS_START,
                "end", AgentConstants.DEFAULT_BUSINESS_HOURS_END));
        account.setLoginStatus(AgentConstants.LOGIN_STATUS_ONLINE);
        account.setStatus(AgentConstants.STATUS_ENABLE);
        account.setLastHeartbeatTime(LocalDateTime.now());
        return account;
    }

    private AgentGeweCredentialDO getCredential(Long credentialId) {
        if (credentialId != null) {
            return validateCredentialExists(credentialId);
        }
        AgentGeweCredentialDO credential = credentialMapper.selectDefaultEnabled();
        if (credential == null) {
            throw exception(GEWE_CREDENTIAL_NOT_EXISTS);
        }
        return credential;
    }

    private AgentGeweCredentialDO validateCredentialExists(Long credentialId) {
        AgentGeweCredentialDO credential = credentialMapper.selectById(credentialId);
        if (credential == null || !Objects.equals(credential.getStatus(), AgentConstants.STATUS_ENABLE)) {
            throw exception(GEWE_CREDENTIAL_NOT_EXISTS);
        }
        return credential;
    }

    private AgentWechatBindSessionDO validateBindSessionExists(Long id) {
        AgentWechatBindSessionDO session = bindSessionMapper.selectById(id);
        if (session == null) {
            throw exception(WECHAT_BIND_SESSION_NOT_EXISTS);
        }
        return session;
    }

    private void updateSessionStatus(AgentWechatBindSessionDO session, String status, String errorMessage) {
        AgentWechatBindSessionDO update = new AgentWechatBindSessionDO();
        update.setId(session.getId());
        update.setStatus(status);
        update.setErrorMessage(errorMessage);
        bindSessionMapper.updateById(update);
        session.setStatus(status);
        session.setErrorMessage(errorMessage);
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
