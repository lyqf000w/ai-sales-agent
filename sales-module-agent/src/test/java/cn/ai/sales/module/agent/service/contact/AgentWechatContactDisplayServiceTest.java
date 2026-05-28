package cn.ai.sales.module.agent.service.contact;

import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.service.gewe.GeweContactInfo;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentWechatContactDisplayServiceTest {

    private final AgentWechatAccountMapper accountMapper = mock(AgentWechatAccountMapper.class);
    private final AgentWechatContactMapper contactMapper = mock(AgentWechatContactMapper.class);
    private final GeweMessageClient geweMessageClient = mock(GeweMessageClient.class);
    private final AgentWechatContactDisplayService service = newService();

    @Test
    void refreshDisplayNameFetchesNicknameFromGeweAndWritesBack() {
        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setId(1L);
        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(2L);
        contact.setWechatAccountId(1L);
        contact.setExternalUserId("wxid_customer");
        contact.setWechatId("wxid_customer");
        contact.setNickname("wxid_customer");

        when(accountMapper.selectById(1L)).thenReturn(account);
        when(geweMessageClient.getContactInfo(account, "wxid_customer"))
                .thenReturn(new GeweContactInfo("wxid_customer", "张雨", null, "avatar-url"));

        service.refreshDisplayNameIfNeeded(contact);

        assertThat(service.resolveDisplayName(contact)).isEqualTo("张雨");
        assertThat(contact.getNickname()).isEqualTo("张雨");
        ArgumentCaptor<AgentWechatContactDO> captor = ArgumentCaptor.forClass(AgentWechatContactDO.class);
        verify(contactMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(2L);
        assertThat(captor.getValue().getNickname()).isEqualTo("张雨");
        assertThat(captor.getValue().getAvatar()).isEqualTo("avatar-url");
    }

    private AgentWechatContactDisplayService newService() {
        AgentWechatContactDisplayService service = new AgentWechatContactDisplayService();
        ReflectionTestUtils.setField(service, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(service, "contactMapper", contactMapper);
        ReflectionTestUtils.setField(service, "geweMessageClient", geweMessageClient);
        return service;
    }

}
