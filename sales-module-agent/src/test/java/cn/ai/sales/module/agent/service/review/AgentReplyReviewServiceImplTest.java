package cn.ai.sales.module.agent.service.review;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewApproveReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewPageReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewRejectReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewRespVO;
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
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.ai.sales.module.agent.service.gewe.GeweTextSendResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentReplyReviewServiceImplTest {

    private final AgentReplyDecisionMapper decisionMapper = mock(AgentReplyDecisionMapper.class);
    private final AgentMessageMapper messageMapper = mock(AgentMessageMapper.class);
    private final AgentConversationMapper conversationMapper = mock(AgentConversationMapper.class);
    private final AgentWechatAccountMapper accountMapper = mock(AgentWechatAccountMapper.class);
    private final AgentWechatContactMapper contactMapper = mock(AgentWechatContactMapper.class);
    private final GeweMessageClient geweMessageClient = mock(GeweMessageClient.class);
    private final AgentReplyReviewServiceImpl service = newService();

    @Test
    void getReviewPageAddsContactIdAndTranslatesEmptyGeneratorReason() {
        AgentReplyDecisionDO decision = new AgentReplyDecisionDO();
        decision.setId(100L);
        decision.setConversationId(1L);
        decision.setSuggestedMessageId(10L);
        decision.setDecisionReason("AI reply generator returned empty content");
        decision.setReviewStatus(AgentConstants.REVIEW_STATUS_PENDING);
        AgentReplyReviewPageReqVO reqVO = new AgentReplyReviewPageReqVO();
        reqVO.setReviewStatus(AgentConstants.REVIEW_STATUS_PENDING);

        when(decisionMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(decision), 1L));
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(1L);
        conversation.setContactId(3L);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);
        AgentMessageDO message = new AgentMessageDO();
        message.setId(10L);
        message.setContent("建议客户先确认需求");
        when(messageMapper.selectById(10L)).thenReturn(message);

        PageResult<AgentReplyReviewRespVO> page = service.getReviewPage(reqVO);

        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).getContactId()).isEqualTo(3L);
        assertThat(page.getList().get(0).getSuggestedContent()).isEqualTo("建议客户先确认需求");
        assertThat(page.getList().get(0).getDecisionReason()).contains("AI 没有生成可发送的回复");
    }

    @Test
    void approveSendsEditedContentAndMarksDecisionEdited() {
        stubPendingDecision();
        when(geweMessageClient.canSend(account())).thenReturn(true);
        when(geweMessageClient.sendText(account(), "friend-1", "修改后的回复"))
                .thenReturn(GeweTextSendResult.success("gewe-msg-1", Map.of("ok", true)));

        AgentReplyReviewApproveReqVO reqVO = new AgentReplyReviewApproveReqVO();
        reqVO.setDecisionId(100L);
        reqVO.setContent("修改后的回复");

        Long messageId = service.approve(reqVO);

        assertThat(messageId).isEqualTo(10L);
        ArgumentCaptor<AgentMessageDO> messageCaptor = ArgumentCaptor.forClass(AgentMessageDO.class);
        verify(messageMapper).updateById(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("修改后的回复");
        assertThat(messageCaptor.getValue().getSendStatus()).isEqualTo(AgentConstants.SEND_STATUS_SENT);
        assertThat(messageCaptor.getValue().getGeweMessageId()).isEqualTo("gewe-msg-1");

        ArgumentCaptor<AgentReplyDecisionDO> decisionCaptor = ArgumentCaptor.forClass(AgentReplyDecisionDO.class);
        verify(decisionMapper).updateById(decisionCaptor.capture());
        assertThat(decisionCaptor.getValue().getId()).isEqualTo(100L);
        assertThat(decisionCaptor.getValue().getSentMessageId()).isEqualTo(10L);
        assertThat(decisionCaptor.getValue().getReviewStatus()).isEqualTo(AgentConstants.REVIEW_STATUS_EDITED);
    }

    @Test
    void rejectRecordsReasonAndDoesNotSendMessage() {
        stubPendingDecision();

        AgentReplyReviewRejectReqVO reqVO = new AgentReplyReviewRejectReqVO();
        reqVO.setDecisionId(100L);
        reqVO.setReason("报价口径不合适");

        service.reject(reqVO);

        verify(geweMessageClient, never()).sendText(any(AgentWechatAccountDO.class), eq("friend-1"), anyString());

        ArgumentCaptor<AgentMessageDO> messageCaptor = ArgumentCaptor.forClass(AgentMessageDO.class);
        verify(messageMapper).updateById(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(messageCaptor.getValue().getSendStatus()).isEqualTo(AgentConstants.SEND_STATUS_REJECTED);
        assertThat(messageCaptor.getValue().getAuditNote()).contains("报价口径不合适");

        ArgumentCaptor<AgentReplyDecisionDO> decisionCaptor = ArgumentCaptor.forClass(AgentReplyDecisionDO.class);
        verify(decisionMapper).updateById(decisionCaptor.capture());
        assertThat(decisionCaptor.getValue().getReviewStatus()).isEqualTo(AgentConstants.REVIEW_STATUS_REJECTED);
        assertThat(decisionCaptor.getValue().getReviewNote()).isEqualTo("报价口径不合适");
    }

    private AgentReplyReviewServiceImpl newService() {
        AgentReplyReviewServiceImpl service = new AgentReplyReviewServiceImpl();
        ReflectionTestUtils.setField(service, "decisionMapper", decisionMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "conversationMapper", conversationMapper);
        ReflectionTestUtils.setField(service, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(service, "contactMapper", contactMapper);
        ReflectionTestUtils.setField(service, "geweMessageClient", geweMessageClient);
        return service;
    }

    private void stubPendingDecision() {
        AgentReplyDecisionDO decision = new AgentReplyDecisionDO();
        decision.setId(100L);
        decision.setConversationId(1L);
        decision.setSuggestedMessageId(10L);
        decision.setReviewStatus(AgentConstants.REVIEW_STATUS_PENDING);
        when(decisionMapper.selectById(100L)).thenReturn(decision);

        AgentMessageDO message = new AgentMessageDO();
        message.setId(10L);
        message.setConversationId(1L);
        message.setWechatAccountId(2L);
        message.setContactId(3L);
        message.setContent("原始建议");
        message.setSendStatus(AgentConstants.SEND_STATUS_PENDING_REVIEW);
        when(messageMapper.selectById(10L)).thenReturn(message);

        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(1L);
        conversation.setWechatAccountId(2L);
        conversation.setContactId(3L);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);

        when(accountMapper.selectById(2L)).thenReturn(account());
        when(contactMapper.selectById(3L)).thenReturn(contact());
    }

    private AgentWechatAccountDO account() {
        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setId(2L);
        account.setGeweApiBaseUrl("http://gewe.local");
        account.setGeweToken("token");
        return account;
    }

    private AgentWechatContactDO contact() {
        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(3L);
        contact.setExternalUserId("friend-1");
        return contact;
    }

}
