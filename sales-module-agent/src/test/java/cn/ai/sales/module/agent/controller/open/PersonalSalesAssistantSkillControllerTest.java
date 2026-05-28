package cn.ai.sales.module.agent.controller.open;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.contact.AgentWechatContactDisplayService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonalSalesAssistantSkillControllerTest {

    private final AgentWechatContactMapper contactMapper = mock(AgentWechatContactMapper.class);
    private final AgentConversationMapper conversationMapper = mock(AgentConversationMapper.class);
    private final AgentWechatContactDisplayService contactDisplayService = mock(AgentWechatContactDisplayService.class);
    private final PersonalSalesAssistantSkillController controller = newController();

    @Test
    @SuppressWarnings("unchecked")
    void customerThreadsReturnsReadableDisplayNameToFrontis() {
        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(3L);
        contact.setWechatAccountId(2L);
        contact.setExternalUserId("wxid_customer");
        contact.setWechatId("wxid_customer");
        contact.setNickname("wxid_customer");

        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(4L);
        conversation.setWechatAccountId(2L);
        conversation.setContactId(3L);

        when(contactMapper.selectConversationQueuePage(any(AgentWechatContactPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(contact), 1L));
        when(conversationMapper.selectByAccountAndContact(2L, 3L)).thenReturn(conversation);
        when(contactDisplayService.resolveDisplayName(contact)).thenReturn("Rain Zhang");

        Map<String, Object> result = ReflectionTestUtils.invokeMethod(controller, "customerThreads",
                Map.of("limit", 10));
        List<Map<String, Object>> threads = (List<Map<String, Object>>) result.get("threads");

        assertThat(threads).hasSize(1);
        assertThat(threads.get(0).get("customer_name")).isEqualTo("Rain Zhang");
        assertThat(threads.get(0).get("display_name")).isEqualTo("Rain Zhang");
        assertThat(threads.get(0).get("customer_name")).isNotEqualTo("wxid_customer");
        verify(contactDisplayService).refreshDisplayNameIfNeeded(contact);
        verify(contactDisplayService).resolveDisplayName(contact);
    }

    @Test
    @SuppressWarnings("unchecked")
    void customerThreadsCanUseFocusQueueForImportantUsers() {
        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(3L);
        contact.setWechatAccountId(2L);
        contact.setFollowUpPriority(AgentConstants.FOLLOW_UP_PRIORITY_FOCUS);

        when(contactMapper.selectConversationQueuePage(any(AgentWechatContactPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(contact), 1L));
        when(contactDisplayService.resolveDisplayName(contact)).thenReturn("重点客户");

        Map<String, Object> result = ReflectionTestUtils.invokeMethod(controller, "customerThreads",
                Map.of("limit", 10, "queue_type", "FOCUS"));
        List<Map<String, Object>> threads = (List<Map<String, Object>>) result.get("threads");

        assertThat(threads).hasSize(1);
        assertThat(threads.get(0).get("follow_up_priority")).isEqualTo(AgentConstants.FOLLOW_UP_PRIORITY_FOCUS);
        verify(contactMapper).selectConversationQueuePage(argThat(reqVO ->
                AgentConstants.CONVERSATION_QUEUE_FOCUS.equals(reqVO.getQueueType())));
        verify(contactDisplayService).refreshDisplayNameIfNeeded(contact);
    }

    @Test
    void addImportantCustomerUpdatesFollowUpPriorityToFocus() {
        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(3L);
        contact.setWechatAccountId(2L);
        contact.setFollowUpPriority(AgentConstants.FOLLOW_UP_PRIORITY_NORMAL);

        when(contactMapper.selectById(3L)).thenReturn(contact);
        when(contactDisplayService.resolveDisplayName(contact)).thenReturn("重点客户");

        Map<String, Object> result = ReflectionTestUtils.invokeMethod(controller, "replyPolicySet",
                Map.of("contact_id", 3L, "important", true));

        assertThat(result.get("important")).isEqualTo(true);
        assertThat(result.get("contact_id")).isEqualTo(3L);
        ArgumentCaptor<AgentWechatContactDO> updateCaptor = ArgumentCaptor.forClass(AgentWechatContactDO.class);
        verify(contactMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(3L);
        assertThat(updateCaptor.getValue().getFollowUpPriority()).isEqualTo(AgentConstants.FOLLOW_UP_PRIORITY_FOCUS);
    }

    @Test
    void removeImportantCustomerUpdatesFollowUpPriorityToNormal() {
        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(3L);
        contact.setWechatAccountId(2L);
        contact.setFollowUpPriority(AgentConstants.FOLLOW_UP_PRIORITY_FOCUS);

        when(contactMapper.selectById(3L)).thenReturn(contact);
        when(contactDisplayService.resolveDisplayName(contact)).thenReturn("普通客户");

        Map<String, Object> result = ReflectionTestUtils.invokeMethod(controller, "replyPolicySet",
                Map.of("contact_id", 3L, "important", false));

        assertThat(result.get("important")).isEqualTo(false);
        assertThat(result.get("contact_id")).isEqualTo(3L);
        ArgumentCaptor<AgentWechatContactDO> updateCaptor = ArgumentCaptor.forClass(AgentWechatContactDO.class);
        verify(contactMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(3L);
        assertThat(updateCaptor.getValue().getFollowUpPriority()).isEqualTo(AgentConstants.FOLLOW_UP_PRIORITY_NORMAL);
    }

    @Test
    void removeImportantCustomerDoesNotDowngradeUrgentCustomer() {
        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(3L);
        contact.setWechatAccountId(2L);
        contact.setFollowUpPriority(AgentConstants.FOLLOW_UP_PRIORITY_URGENT);

        when(contactMapper.selectById(3L)).thenReturn(contact);
        when(contactDisplayService.resolveDisplayName(contact)).thenReturn("紧急客户");

        Map<String, Object> result = ReflectionTestUtils.invokeMethod(controller, "replyPolicySet",
                Map.of("contact_id", 3L, "important", false));

        assertThat(result.get("updated")).isEqualTo(false);
        assertThat(result.get("follow_up_priority")).isEqualTo(AgentConstants.FOLLOW_UP_PRIORITY_URGENT);
        verify(contactMapper, never()).updateById(any(AgentWechatContactDO.class));
    }

    private PersonalSalesAssistantSkillController newController() {
        PersonalSalesAssistantSkillController controller = new PersonalSalesAssistantSkillController();
        ReflectionTestUtils.setField(controller, "properties", new PersonalSalesAssistantSkillProperties());
        ReflectionTestUtils.setField(controller, "contactMapper", contactMapper);
        ReflectionTestUtils.setField(controller, "conversationMapper", conversationMapper);
        ReflectionTestUtils.setField(controller, "contactDisplayService", contactDisplayService);
        return controller;
    }

}
