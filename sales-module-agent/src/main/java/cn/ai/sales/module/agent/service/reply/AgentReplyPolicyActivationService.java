package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.framework.security.core.util.SecurityFrameworkUtils;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentReplyDecisionDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentReplyDecisionMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
public class AgentReplyPolicyActivationService {

    @Resource
    private AgentConversationMapper conversationMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private AgentReplyDecisionMapper decisionMapper;
    @Resource
    private AgentMessageMapper messageMapper;

    public void activateContactAutoReply(Long wechatAccountId, Long contactId, Long conversationId) {
        if (contactId == null) {
            return;
        }
        AgentConversationDO conversation = conversationId == null ? null : conversationMapper.selectById(conversationId);
        if (conversation == null && wechatAccountId != null) {
            conversation = conversationMapper.selectByAccountAndContact(wechatAccountId, contactId);
        }
        if (conversation != null) {
            rejectPendingSuggestions(conversation.getId());
            conversationMapper.resetForAutoReply(conversation.getId());
        }
        contactMapper.resetConversationStateForAutoReply(contactId);
    }

    public void activateAccountAutoReply(Long wechatAccountId) {
        if (wechatAccountId == null) {
            return;
        }
        List<AgentConversationDO> conversations = conversationMapper.selectListByWechatAccountId(wechatAccountId);
        conversations.forEach(conversation -> rejectPendingSuggestions(conversation.getId()));
        conversationMapper.resetForAutoReplyByWechatAccountId(wechatAccountId);
        contactMapper.resetConversationStateForAutoReplyByWechatAccountId(wechatAccountId);
    }

    private void rejectPendingSuggestions(Long conversationId) {
        List<AgentReplyDecisionDO> pendingDecisions = decisionMapper.selectPendingListByConversationId(conversationId);
        for (AgentReplyDecisionDO decision : pendingDecisions) {
            rejectSuggestedMessage(decision.getSuggestedMessageId());
            rejectDecision(decision.getId());
        }
    }

    private void rejectSuggestedMessage(Long messageId) {
        if (messageId == null) {
            return;
        }
        AgentMessageDO messageUpdate = new AgentMessageDO();
        messageUpdate.setId(messageId);
        messageUpdate.setSendStatus(AgentConstants.SEND_STATUS_REJECTED);
        messageUpdate.setAuditNote("自动回复策略已启用，旧 AI 建议作废");
        messageUpdate.setOperatorUserId(getLoginUserIdQuietly());
        messageMapper.updateById(messageUpdate);
    }

    private void rejectDecision(Long decisionId) {
        AgentReplyDecisionDO decisionUpdate = new AgentReplyDecisionDO();
        decisionUpdate.setId(decisionId);
        decisionUpdate.setReviewStatus(AgentConstants.REVIEW_STATUS_REJECTED);
        decisionUpdate.setReviewNote("自动回复策略已启用，旧 AI 建议作废");
        decisionUpdate.setReviewUserId(getLoginUserIdQuietly());
        decisionUpdate.setReviewTime(LocalDateTime.now());
        decisionMapper.updateById(decisionUpdate);
    }

    private Long getLoginUserIdQuietly() {
        try {
            return SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception ignored) {
            return null;
        }
    }
}
