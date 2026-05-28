package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentReplyDecisionDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentReplyDecisionMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentReplyPolicyActivationServiceTest {

    private final AgentConversationMapper conversationMapper = mock(AgentConversationMapper.class);
    private final AgentWechatContactMapper contactMapper = mock(AgentWechatContactMapper.class);
    private final AgentReplyDecisionMapper decisionMapper = mock(AgentReplyDecisionMapper.class);
    private final AgentMessageMapper messageMapper = mock(AgentMessageMapper.class);
    private final AgentReplyPolicyActivationService service = newService();

    @Test
    void activateContactAutoReplyResetsConversationAndRejectsOldPendingSuggestion() {
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(1L);
        conversation.setWechatAccountId(2L);
        conversation.setContactId(3L);
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);

        AgentReplyDecisionDO decision = new AgentReplyDecisionDO();
        decision.setId(10L);
        decision.setSuggestedMessageId(20L);
        when(decisionMapper.selectPendingListByConversationId(1L)).thenReturn(List.of(decision));

        service.activateContactAutoReply(2L, 3L, 1L);

        verify(conversationMapper).resetForAutoReply(1L);
        verify(contactMapper).resetConversationStateForAutoReply(3L);

        ArgumentCaptor<AgentMessageDO> messageCaptor = ArgumentCaptor.forClass(AgentMessageDO.class);
        verify(messageMapper).updateById(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getId()).isEqualTo(20L);
        assertThat(messageCaptor.getValue().getSendStatus()).isEqualTo(AgentConstants.SEND_STATUS_REJECTED);

        ArgumentCaptor<AgentReplyDecisionDO> decisionCaptor = ArgumentCaptor.forClass(AgentReplyDecisionDO.class);
        verify(decisionMapper).updateById(decisionCaptor.capture());
        assertThat(decisionCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(decisionCaptor.getValue().getReviewStatus()).isEqualTo(AgentConstants.REVIEW_STATUS_REJECTED);
        assertThat(decisionCaptor.getValue().getReviewTime()).isNotNull();
    }

    @Test
    void activateAccountAutoReplyResetsAllAccountConversations() {
        AgentConversationDO first = new AgentConversationDO();
        first.setId(1L);
        AgentConversationDO second = new AgentConversationDO();
        second.setId(2L);
        when(conversationMapper.selectListByWechatAccountId(9L)).thenReturn(List.of(first, second));
        when(decisionMapper.selectPendingListByConversationId(1L)).thenReturn(List.of());
        when(decisionMapper.selectPendingListByConversationId(2L)).thenReturn(List.of());

        service.activateAccountAutoReply(9L);

        verify(conversationMapper).resetForAutoReplyByWechatAccountId(9L);
        verify(contactMapper).resetConversationStateForAutoReplyByWechatAccountId(9L);
    }

    private AgentReplyPolicyActivationService newService() {
        AgentReplyPolicyActivationService service = new AgentReplyPolicyActivationService();
        ReflectionTestUtils.setField(service, "conversationMapper", conversationMapper);
        ReflectionTestUtils.setField(service, "contactMapper", contactMapper);
        ReflectionTestUtils.setField(service, "decisionMapper", decisionMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        return service;
    }
}
