package cn.ai.sales.module.agent.service.account;

import cn.ai.sales.module.agent.controller.admin.account.vo.AgentGeweCredentialSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentGeweCredentialDO;
import cn.ai.sales.module.agent.dal.mysql.AgentGeweCredentialMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentGeweCredentialServiceImplTest {

    private final AgentGeweCredentialMapper credentialMapper = mock(AgentGeweCredentialMapper.class);
    private final GeweMessageClient geweMessageClient = mock(GeweMessageClient.class);
    private final AgentGeweCredentialServiceImpl service = newService();

    @Test
    void createCredentialInsertsNewCredentialWhenIdIsBlank() {
        AgentGeweCredentialSaveReqVO reqVO = saveReqVO();

        service.saveCredential(reqVO);

        ArgumentCaptor<AgentGeweCredentialDO> captor = ArgumentCaptor.forClass(AgentGeweCredentialDO.class);
        verify(credentialMapper).insert(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("华东 GeWe");
        assertThat(captor.getValue().getCallbackToken()).isNotBlank();
        assertThat(captor.getValue().getCallbackUrl()).contains("/admin-api/agent/gewe/callback/");
        assertThat(captor.getValue().getStatus()).isEqualTo(AgentConstants.STATUS_ENABLE);
        verify(credentialMapper, never()).selectFirst();
    }

    @Test
    void updateCredentialKeepsOldTokenWhenTokenIsBlank() {
        AgentGeweCredentialDO existed = new AgentGeweCredentialDO();
        existed.setId(10L);
        existed.setGeweToken("old-token");
        existed.setCallbackToken("callback-token");
        when(credentialMapper.selectById(10L)).thenReturn(existed);

        AgentGeweCredentialSaveReqVO reqVO = saveReqVO();
        reqVO.setId(10L);
        reqVO.setGeweToken("");
        reqVO.setCallbackUrl("https://sales.example.com/admin-api/agent/gewe/callback/callback-token");

        service.saveCredential(reqVO);

        ArgumentCaptor<AgentGeweCredentialDO> captor = ArgumentCaptor.forClass(AgentGeweCredentialDO.class);
        verify(credentialMapper).updateById(captor.capture());
        assertThat(captor.getValue().getGeweToken()).isEqualTo("old-token");
        assertThat(captor.getValue().getCallbackToken()).isEqualTo("callback-token");
    }

    @Test
    void getEnabledCredentialsReturnsEnabledList() {
        AgentGeweCredentialDO credential = new AgentGeweCredentialDO();
        credential.setId(10L);
        when(credentialMapper.selectEnabledList()).thenReturn(List.of(credential));

        assertThat(service.getEnabledCredentialList()).containsExactly(credential);
    }

    private AgentGeweCredentialServiceImpl newService() {
        AgentGeweCredentialServiceImpl service = new AgentGeweCredentialServiceImpl();
        ReflectionTestUtils.setField(service, "credentialMapper", credentialMapper);
        ReflectionTestUtils.setField(service, "geweMessageClient", geweMessageClient);
        return service;
    }

    private AgentGeweCredentialSaveReqVO saveReqVO() {
        AgentGeweCredentialSaveReqVO reqVO = new AgentGeweCredentialSaveReqVO();
        reqVO.setName("华东 GeWe");
        reqVO.setGeweApiBaseUrl("https://api.geweapi.com");
        reqVO.setGeweToken("new-token");
        reqVO.setStatus(AgentConstants.STATUS_ENABLE);
        return reqVO;
    }

}
