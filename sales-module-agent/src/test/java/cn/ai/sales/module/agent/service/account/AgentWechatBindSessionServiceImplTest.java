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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentWechatBindSessionServiceImplTest {

    private final AgentGeweCredentialMapper credentialMapper = mock(AgentGeweCredentialMapper.class);
    private final AgentWechatBindSessionMapper bindSessionMapper = mock(AgentWechatBindSessionMapper.class);
    private final AgentWechatAccountMapper accountMapper = mock(AgentWechatAccountMapper.class);
    private final GeweMessageClient geweMessageClient = mock(GeweMessageClient.class);
    private final AgentWechatBindSessionServiceImpl service = newService();

    @Test
    void createBindSessionStoresQrCodeFromDefaultCredential() {
        AgentGeweCredentialDO credential = credential();
        when(credentialMapper.selectDefaultEnabled()).thenReturn(credential);
        when(geweMessageClient.getLoginQrCode(credential))
                .thenReturn(new GeweLoginQrCodeResult("wx_app_1", "uuid-1",
                        "http://weixin.qq.com/x/abc", "data:image/jpg;base64,xxx", Map.of()));

        AgentWechatBindSessionCreateReqVO reqVO = new AgentWechatBindSessionCreateReqVO();
        reqVO.setAgentId(2L);
        reqVO.setOwnerUserId(3L);

        service.createBindSession(reqVO);

        ArgumentCaptor<AgentWechatBindSessionDO> captor = ArgumentCaptor.forClass(AgentWechatBindSessionDO.class);
        verify(bindSessionMapper).insert(captor.capture());
        assertThat(captor.getValue().getCredentialId()).isEqualTo(10L);
        assertThat(captor.getValue().getAgentId()).isEqualTo(2L);
        assertThat(captor.getValue().getOwnerUserId()).isEqualTo(3L);
        assertThat(captor.getValue().getAppId()).isEqualTo("wx_app_1");
        assertThat(captor.getValue().getUuid()).isEqualTo("uuid-1");
        assertThat(captor.getValue().getQrData()).isEqualTo("http://weixin.qq.com/x/abc");
        assertThat(captor.getValue().getQrImgBase64()).startsWith("data:image/jpg;base64,");
        assertThat(captor.getValue().getStatus()).isEqualTo(AgentConstants.BIND_SESSION_WAIT_SCAN);
        assertThat(captor.getValue().getExpiresAt()).isNotNull();
    }

    @Test
    void checkBindSessionCreatesWechatAccountWhenLoginSucceeds() {
        AgentGeweCredentialDO credential = credential();
        AgentWechatBindSessionDO session = bindSession();
        when(bindSessionMapper.selectById(100L)).thenReturn(session);
        when(credentialMapper.selectById(10L)).thenReturn(credential);
        when(geweMessageClient.checkLogin(credential, "wx_app_1", "uuid-1"))
                .thenReturn(new GeweLoginCheckResult(true, false, null, "G", "http://avatar", Map.of()));
        when(geweMessageClient.getProfile(credential, "wx_app_1"))
                .thenReturn(new GeweProfileResult("wxid_owner", "销售微信", "http://big-avatar", "13800000000", Map.of()));
        when(accountMapper.selectByCredentialAndAppId(10L, "wx_app_1")).thenReturn(null);
        when(accountMapper.selectByWxid("wxid_owner")).thenReturn(null);
        doAnswer(invocation -> {
            AgentWechatAccountDO account = invocation.getArgument(0);
            account.setId(200L);
            return 1;
        }).when(accountMapper).insert(any(AgentWechatAccountDO.class));

        AgentWechatBindSessionDO result = service.checkBindSession(100L);

        ArgumentCaptor<AgentWechatAccountDO> accountCaptor = ArgumentCaptor.forClass(AgentWechatAccountDO.class);
        verify(accountMapper).insert(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getGeweCredentialId()).isEqualTo(10L);
        assertThat(accountCaptor.getValue().getCallbackToken()).isNotBlank();
        assertThat(accountCaptor.getValue().getCallbackUrl())
                .isEqualTo("/api/v1/gewechat/callback?token=callback-token");
        assertThat(accountCaptor.getValue().getGeweToken()).isNull();
        assertThat(accountCaptor.getValue().getGeweApiBaseUrl()).isNull();
        assertThat(accountCaptor.getValue().getAgentId()).isEqualTo(2L);
        assertThat(accountCaptor.getValue().getOwnerUserId()).isEqualTo(3L);
        assertThat(accountCaptor.getValue().getGeweAppId()).isEqualTo("wx_app_1");
        assertThat(accountCaptor.getValue().getWechatId()).isEqualTo("wxid_owner");
        assertThat(accountCaptor.getValue().getNickname()).isEqualTo("销售微信");
        assertThat(accountCaptor.getValue().getAvatar()).isEqualTo("http://big-avatar");
        assertThat(accountCaptor.getValue().getQuietSeconds()).isEqualTo(AgentConstants.DEFAULT_QUIET_SECONDS);
        assertThat(accountCaptor.getValue().getBusinessHours())
                .containsEntry("start", "08:00")
                .containsEntry("end", "22:00");
        assertThat(accountCaptor.getValue().getLoginStatus()).isEqualTo(AgentConstants.LOGIN_STATUS_ONLINE);
        assertThat(accountCaptor.getValue().getStatus()).isEqualTo(AgentConstants.STATUS_ENABLE);

        ArgumentCaptor<AgentWechatBindSessionDO> sessionCaptor = ArgumentCaptor.forClass(AgentWechatBindSessionDO.class);
        verify(bindSessionMapper).updateById(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(AgentConstants.BIND_SESSION_BOUND);
        assertThat(sessionCaptor.getValue().getBindAccountId()).isEqualTo(200L);
        assertThat(result.getStatus()).isEqualTo(AgentConstants.BIND_SESSION_BOUND);
        assertThat(result.getBindAccountId()).isEqualTo(200L);
    }

    @Test
    void checkBindSessionPreservesExistingAccountCallbackTokenWhenRebinding() {
        AgentGeweCredentialDO credential = credential();
        AgentWechatBindSessionDO session = bindSession();
        AgentWechatAccountDO existedAccount = new AgentWechatAccountDO();
        existedAccount.setId(200L);
        existedAccount.setCallbackToken("account-token");
        existedAccount.setCallbackSecret("account-secret");
        when(bindSessionMapper.selectById(100L)).thenReturn(session);
        when(credentialMapper.selectById(10L)).thenReturn(credential);
        when(geweMessageClient.checkLogin(credential, "wx_app_1", "uuid-1"))
                .thenReturn(new GeweLoginCheckResult(true, false, null, "G", "http://avatar", Map.of()));
        when(geweMessageClient.getProfile(credential, "wx_app_1"))
                .thenReturn(new GeweProfileResult("wxid_owner", "销售微信", "http://big-avatar", "13800000000", Map.of()));
        when(accountMapper.selectByCredentialAndAppId(10L, "wx_app_1")).thenReturn(existedAccount);

        service.checkBindSession(100L);

        ArgumentCaptor<AgentWechatAccountDO> accountCaptor = ArgumentCaptor.forClass(AgentWechatAccountDO.class);
        verify(accountMapper).updateById(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getId()).isEqualTo(200L);
        assertThat(accountCaptor.getValue().getCallbackToken()).isEqualTo("account-token");
        assertThat(accountCaptor.getValue().getCallbackSecret()).isEqualTo("account-secret");
        assertThat(accountCaptor.getValue().getCallbackUrl())
                .isEqualTo("/api/v1/gewechat/callback?token=callback-token");
    }

    @Test
    void checkBindSessionMarksScannedLoginAsWaitingConfirm() {
        AgentGeweCredentialDO credential = credential();
        AgentWechatBindSessionDO session = bindSession();
        when(bindSessionMapper.selectById(100L)).thenReturn(session);
        when(credentialMapper.selectById(10L)).thenReturn(credential);
        when(geweMessageClient.checkLogin(credential, "wx_app_1", "uuid-1"))
                .thenReturn(new GeweLoginCheckResult(false, true, "https://verify.example/qr",
                        "G", "http://avatar", Map.of()));

        AgentWechatBindSessionDO result = service.checkBindSession(100L);

        ArgumentCaptor<AgentWechatBindSessionDO> sessionCaptor = ArgumentCaptor.forClass(AgentWechatBindSessionDO.class);
        verify(bindSessionMapper).updateById(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(AgentConstants.BIND_SESSION_WAIT_CONFIRM);
        assertThat(sessionCaptor.getValue().getVerifyUrl()).isEqualTo("https://verify.example/qr");
        assertThat(result.getStatus()).isEqualTo(AgentConstants.BIND_SESSION_WAIT_CONFIRM);
    }

    private AgentWechatBindSessionServiceImpl newService() {
        AgentWechatBindSessionServiceImpl service = new AgentWechatBindSessionServiceImpl();
        ReflectionTestUtils.setField(service, "credentialMapper", credentialMapper);
        ReflectionTestUtils.setField(service, "bindSessionMapper", bindSessionMapper);
        ReflectionTestUtils.setField(service, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(service, "geweMessageClient", geweMessageClient);
        return service;
    }

    private AgentGeweCredentialDO credential() {
        AgentGeweCredentialDO credential = new AgentGeweCredentialDO();
        credential.setId(10L);
        credential.setName("默认 GeWe");
        credential.setGeweApiBaseUrl("http://api.geweapi.com");
        credential.setGeweToken("token");
        credential.setCallbackToken("callback-token");
        credential.setStatus(AgentConstants.STATUS_ENABLE);
        return credential;
    }

    private AgentWechatBindSessionDO bindSession() {
        AgentWechatBindSessionDO session = new AgentWechatBindSessionDO();
        session.setId(100L);
        session.setCredentialId(10L);
        session.setAgentId(2L);
        session.setOwnerUserId(3L);
        session.setAppId("wx_app_1");
        session.setUuid("uuid-1");
        session.setStatus(AgentConstants.BIND_SESSION_WAIT_SCAN);
        return session;
    }

}
