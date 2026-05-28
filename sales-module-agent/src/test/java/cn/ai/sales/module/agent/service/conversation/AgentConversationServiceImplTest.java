package cn.ai.sales.module.agent.service.conversation;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactPageReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationContactRespVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationSendMessageReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationRestoreOriginalPolicyReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentReplyDecisionDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentReplyDecisionMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.contact.AgentWechatContactDisplayService;
import cn.ai.sales.module.agent.service.gewe.GeweContactInfo;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.ai.sales.module.agent.service.gewe.GeweTextSendResult;
import cn.ai.sales.module.agent.service.review.AgentReplyReviewService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentConversationServiceImplTest {

    private final AgentConversationMapper conversationMapper = mock(AgentConversationMapper.class);
    private final AgentMessageMapper messageMapper = mock(AgentMessageMapper.class);
    private final AgentReplyDecisionMapper decisionMapper = mock(AgentReplyDecisionMapper.class);
    private final AgentWechatAccountMapper accountMapper = mock(AgentWechatAccountMapper.class);
    private final AgentWechatContactMapper contactMapper = mock(AgentWechatContactMapper.class);
    private final GeweMessageClient geweMessageClient = mock(GeweMessageClient.class);
    private final AgentReplyReviewService replyReviewService = mock(AgentReplyReviewService.class);
    private final AgentWechatContactDisplayService contactDisplayService = mock(AgentWechatContactDisplayService.class);
    private final AgentConversationServiceImpl service = newService();

    @Test
    void getConversationContactPageReturnsReadableDisplayNameToConsole() {
        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(3L);
        contact.setWechatAccountId(2L);
        contact.setExternalUserId("wxid_customer");
        contact.setWechatId("wxid_customer");
        contact.setNickname("wxid_customer");

        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(4L);
        conversation.setAgentId(5L);
        conversation.setWechatAccountId(2L);
        conversation.setContactId(3L);
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        conversation.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);

        when(contactMapper.selectConversationQueuePage(any(AgentWechatContactPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(contact), 1L));
        when(conversationMapper.selectByAccountAndContact(2L, 3L)).thenReturn(conversation);
        when(contactDisplayService.resolveDisplayName(contact)).thenReturn("Rain Zhang");

        PageResult<AgentConversationContactRespVO> page =
                service.getConversationContactPage(new AgentWechatContactPageReqVO());

        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).getDisplayName()).isEqualTo("Rain Zhang");
        assertThat(page.getList().get(0).getDisplayName()).doesNotContain("wxid");
        verify(contactDisplayService).resolveDisplayName(contact);
    }

    @Test
    void sendManualMessageResolvesWaitingConfirmAndInvalidatesPendingAiSuggestion() {
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(1L);
        conversation.setWechatAccountId(2L);
        conversation.setContactId(3L);
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);

        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setId(2L);
        account.setGeweApiBaseUrl("http://gewe.local");
        account.setGeweToken("token");
        when(accountMapper.selectById(2L)).thenReturn(account);
        when(geweMessageClient.canSend(account)).thenReturn(true);
        when(geweMessageClient.sendText(account, "friend-1", "人工已经回复客户"))
                .thenReturn(GeweTextSendResult.success("human-gewe-msg", Map.of("ok", true)));

        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(3L);
        contact.setExternalUserId("friend-1");
        when(contactMapper.selectById(3L)).thenReturn(contact);

        AgentReplyDecisionDO pendingDecision = new AgentReplyDecisionDO();
        pendingDecision.setId(100L);
        pendingDecision.setConversationId(1L);
        pendingDecision.setSuggestedMessageId(10L);
        pendingDecision.setReviewStatus(AgentConstants.REVIEW_STATUS_PENDING);
        when(decisionMapper.selectPendingListByConversationId(1L)).thenReturn(List.of(pendingDecision));

        doAnswer(invocation -> {
            AgentMessageDO message = invocation.getArgument(0);
            message.setId(200L);
            return 1;
        }).when(messageMapper).insert(any(AgentMessageDO.class));

        AgentConversationSendMessageReqVO reqVO = new AgentConversationSendMessageReqVO();
        reqVO.setConversationId(1L);
        reqVO.setContent("人工已经回复客户");

        Long messageId = service.sendMessage(reqVO);

        assertThat(messageId).isEqualTo(200L);

        ArgumentCaptor<AgentMessageDO> pendingMessageCaptor = ArgumentCaptor.forClass(AgentMessageDO.class);
        verify(messageMapper).updateById(pendingMessageCaptor.capture());
        assertThat(pendingMessageCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(pendingMessageCaptor.getValue().getSendStatus()).isEqualTo(AgentConstants.SEND_STATUS_REJECTED);
        assertThat(pendingMessageCaptor.getValue().getAuditNote()).contains("人工已回复");

        ArgumentCaptor<AgentReplyDecisionDO> decisionCaptor = ArgumentCaptor.forClass(AgentReplyDecisionDO.class);
        verify(decisionMapper).updateById(decisionCaptor.capture());
        assertThat(decisionCaptor.getValue().getId()).isEqualTo(100L);
        assertThat(decisionCaptor.getValue().getReviewStatus()).isEqualTo(AgentConstants.REVIEW_STATUS_REJECTED);
        assertThat(decisionCaptor.getValue().getReviewNote()).contains("人工已回复");

        ArgumentCaptor<AgentConversationDO> conversationCaptor = ArgumentCaptor.forClass(AgentConversationDO.class);
        verify(conversationMapper).updateById(conversationCaptor.capture());
        assertThat(conversationCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(conversationCaptor.getValue().getStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_OPEN);
        assertThat(conversationCaptor.getValue().getLastMessageId()).isEqualTo(200L);

        ArgumentCaptor<AgentWechatContactDO> contactCaptor = ArgumentCaptor.forClass(AgentWechatContactDO.class);
        verify(contactMapper).updateById(contactCaptor.capture());
        assertThat(contactCaptor.getValue().getId()).isEqualTo(3L);
        assertThat(contactCaptor.getValue().getLastConversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_OPEN);
        assertThat(contactCaptor.getValue().getLastMessageTime()).isNotNull();
    }

    @Test
    void restoreOriginalPolicyClearsTemporaryHumanTakeoverWithoutChangingReplyPolicy() {
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(1L);
        conversation.setContactId(3L);
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
        conversation.setRiskLevel(AgentConstants.RISK_LEVEL_RED);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);

        AgentConversationRestoreOriginalPolicyReqVO reqVO = new AgentConversationRestoreOriginalPolicyReqVO();
        reqVO.setConversationId(1L);

        service.restoreOriginalPolicy(reqVO);

        ArgumentCaptor<AgentConversationDO> conversationCaptor = ArgumentCaptor.forClass(AgentConversationDO.class);
        verify(conversationMapper).updateById(conversationCaptor.capture());
        assertThat(conversationCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(conversationCaptor.getValue().getStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_OPEN);
        assertThat(conversationCaptor.getValue().getRiskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_GREEN);
        assertThat(conversationCaptor.getValue().getContinuousAutoReplyCount()).isZero();
        verify(conversationMapper).clearTemporaryTakeover(1L);

        ArgumentCaptor<AgentWechatContactDO> contactCaptor = ArgumentCaptor.forClass(AgentWechatContactDO.class);
        verify(contactMapper).updateById(contactCaptor.capture());
        assertThat(contactCaptor.getValue().getId()).isEqualTo(3L);
        assertThat(contactCaptor.getValue().getRiskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_GREEN);
        assertThat(contactCaptor.getValue().getLastConversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_OPEN);
    }

    @Test
    void acknowledgePendingViewClearsReminderWhenNoActionableSuggestionExists() {
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(1L);
        conversation.setContactId(3L);
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
        conversation.setRiskLevel(AgentConstants.RISK_LEVEL_YELLOW);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);

        AgentReplyDecisionDO pendingDecision = new AgentReplyDecisionDO();
        pendingDecision.setId(100L);
        pendingDecision.setConversationId(1L);
        pendingDecision.setReviewStatus(AgentConstants.REVIEW_STATUS_PENDING);
        when(decisionMapper.selectPendingListByConversationId(1L)).thenReturn(List.of(pendingDecision));

        AgentConversationRestoreOriginalPolicyReqVO reqVO = new AgentConversationRestoreOriginalPolicyReqVO();
        reqVO.setConversationId(1L);

        boolean acknowledged = service.acknowledgePendingView(reqVO);

        assertThat(acknowledged).isTrue();

        ArgumentCaptor<AgentReplyDecisionDO> decisionCaptor = ArgumentCaptor.forClass(AgentReplyDecisionDO.class);
        verify(decisionMapper).updateById(decisionCaptor.capture());
        assertThat(decisionCaptor.getValue().getId()).isEqualTo(100L);
        assertThat(decisionCaptor.getValue().getReviewStatus()).isEqualTo(AgentConstants.REVIEW_STATUS_REJECTED);
        assertThat(decisionCaptor.getValue().getReviewNote()).contains("人工已查看");

        ArgumentCaptor<AgentConversationDO> conversationCaptor = ArgumentCaptor.forClass(AgentConversationDO.class);
        verify(conversationMapper).updateById(conversationCaptor.capture());
        assertThat(conversationCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(conversationCaptor.getValue().getStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_OPEN);
        assertThat(conversationCaptor.getValue().getRiskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_GREEN);
        verify(conversationMapper).clearPendingReply(1L);

        ArgumentCaptor<AgentWechatContactDO> contactCaptor = ArgumentCaptor.forClass(AgentWechatContactDO.class);
        verify(contactMapper).updateById(contactCaptor.capture());
        assertThat(contactCaptor.getValue().getId()).isEqualTo(3L);
        assertThat(contactCaptor.getValue().getLastConversationStatus()).isEqualTo(AgentConstants.CONVERSATION_STATUS_OPEN);
    }

    @Test
    void acknowledgePendingViewKeepsReminderWhenActionableSuggestionExists() {
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(1L);
        conversation.setContactId(3L);
        conversation.setStatus(AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);

        AgentReplyDecisionDO pendingDecision = new AgentReplyDecisionDO();
        pendingDecision.setId(100L);
        pendingDecision.setConversationId(1L);
        pendingDecision.setSuggestedMessageId(10L);
        pendingDecision.setReviewStatus(AgentConstants.REVIEW_STATUS_PENDING);
        when(decisionMapper.selectPendingListByConversationId(1L)).thenReturn(List.of(pendingDecision));

        AgentMessageDO suggestedMessage = new AgentMessageDO();
        suggestedMessage.setId(10L);
        suggestedMessage.setSendStatus(AgentConstants.SEND_STATUS_PENDING_REVIEW);
        when(messageMapper.selectById(10L)).thenReturn(suggestedMessage);

        AgentConversationRestoreOriginalPolicyReqVO reqVO = new AgentConversationRestoreOriginalPolicyReqVO();
        reqVO.setConversationId(1L);

        boolean acknowledged = service.acknowledgePendingView(reqVO);

        assertThat(acknowledged).isFalse();
        verify(decisionMapper, never()).updateById(any(AgentReplyDecisionDO.class));
        verify(conversationMapper, never()).updateById(any(AgentConversationDO.class));
        verify(contactMapper, never()).updateById(any(AgentWechatContactDO.class));
    }

    @Test
    void getConversationMessagesReplacesGroupMemberWxidPrefix() {
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(1L);
        conversation.setWechatAccountId(2L);
        conversation.setContactId(3L);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);

        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setId(2L);
        when(accountMapper.selectById(2L)).thenReturn(account);

        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(3L);
        contact.setExternalUserId("20666784639@chatroom");
        when(contactMapper.selectById(3L)).thenReturn(contact);

        AgentMessageDO message = new AgentMessageDO();
        message.setId(4L);
        message.setConversationId(1L);
        message.setDirection(AgentConstants.MESSAGE_DIRECTION_INBOUND);
        message.setContent("wxid_member:\nhello");
        when(messageMapper.selectListByConversationId(1L)).thenReturn(List.of(message));
        when(geweMessageClient.getChatroomMemberInfo(account, "20666784639@chatroom", "wxid_member"))
                .thenReturn(new GeweContactInfo("wxid_member", "张雨", null, null));

        List<AgentMessageDO> messages = service.getConversationMessages(1L);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("张雨:\nhello");
        ArgumentCaptor<AgentMessageDO> messageCaptor = ArgumentCaptor.forClass(AgentMessageDO.class);
        verify(messageMapper).updateById(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getId()).isEqualTo(4L);
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("张雨:\nhello");
    }

    @Test
    void getConversationMessagesHidesLegacyGroupMessagesFromPrivateConversation() {
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(1L);
        conversation.setWechatAccountId(2L);
        conversation.setContactId(3L);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);

        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setId(2L);
        when(accountMapper.selectById(2L)).thenReturn(account);

        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(3L);
        contact.setExternalUserId("wxid_private");
        when(contactMapper.selectById(3L)).thenReturn(contact);

        AgentMessageDO directMessage = new AgentMessageDO();
        directMessage.setId(4L);
        directMessage.setConversationId(1L);
        directMessage.setDirection(AgentConstants.MESSAGE_DIRECTION_INBOUND);
        directMessage.setContent("hello in private");
        directMessage.setRawPayload(Map.of("fromUser", "wxid_private"));

        AgentMessageDO legacyGroupMessage = new AgentMessageDO();
        legacyGroupMessage.setId(5L);
        legacyGroupMessage.setConversationId(1L);
        legacyGroupMessage.setDirection(AgentConstants.MESSAGE_DIRECTION_INBOUND);
        legacyGroupMessage.setContent("wxid_member:\nhello from group");
        legacyGroupMessage.setRawPayload(Map.of("fromGroup", "20666784639@chatroom"));
        when(messageMapper.selectListByConversationId(1L)).thenReturn(List.of(directMessage, legacyGroupMessage));

        List<AgentMessageDO> messages = service.getConversationMessages(1L);

        assertThat(messages).containsExactly(directMessage);
    }

    @Test
    void getConversationMessagesKeepsPrivateMessageWhenPayloadContainsUnrelatedChatroomText() {
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(1L);
        conversation.setWechatAccountId(2L);
        conversation.setContactId(3L);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);

        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setId(2L);
        when(accountMapper.selectById(2L)).thenReturn(account);

        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(3L);
        contact.setExternalUserId("wxid_private");
        when(contactMapper.selectById(3L)).thenReturn(contact);

        AgentMessageDO privateMessage = new AgentMessageDO();
        privateMessage.setId(4L);
        privateMessage.setConversationId(1L);
        privateMessage.setDirection(AgentConstants.MESSAGE_DIRECTION_INBOUND);
        privateMessage.setContent("hello in private");
        privateMessage.setRawPayload(Map.of(
                "fromUser", "wxid_private",
                "debug", Map.of("lastChatroom", "20666784639@chatroom")));
        when(messageMapper.selectListByConversationId(1L)).thenReturn(List.of(privateMessage));

        List<AgentMessageDO> messages = service.getConversationMessages(1L);

        assertThat(messages).containsExactly(privateMessage);
    }

    private AgentConversationServiceImpl newService() {
        AgentConversationServiceImpl service = new AgentConversationServiceImpl();
        ReflectionTestUtils.setField(service, "conversationMapper", conversationMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "decisionMapper", decisionMapper);
        ReflectionTestUtils.setField(service, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(service, "contactMapper", contactMapper);
        ReflectionTestUtils.setField(service, "geweMessageClient", geweMessageClient);
        ReflectionTestUtils.setField(service, "replyReviewService", replyReviewService);
        ReflectionTestUtils.setField(service, "contactDisplayService", contactDisplayService);
        return service;
    }

}
