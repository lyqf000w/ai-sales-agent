package cn.ai.sales.module.agent.service.review;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.framework.security.core.util.SecurityFrameworkUtils;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationMessageReviewReqVO;
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
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.CONVERSATION_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.GEWE_SEND_CONFIG_MISSING;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.GEWE_SEND_FAILED;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.MESSAGE_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.MESSAGE_STATUS_INVALID;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.REPLY_DECISION_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.REPLY_DECISION_STATUS_INVALID;

@Service
@Validated
public class AgentReplyReviewServiceImpl implements AgentReplyReviewService {

    @Resource
    private AgentReplyDecisionMapper decisionMapper;
    @Resource
    private AgentMessageMapper messageMapper;
    @Resource
    private AgentConversationMapper conversationMapper;
    @Resource
    private AgentWechatAccountMapper accountMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private GeweMessageClient geweMessageClient;

    @Override
    public PageResult<AgentReplyReviewRespVO> getReviewPage(AgentReplyReviewPageReqVO pageReqVO) {
        PageResult<AgentReplyDecisionDO> pageResult = decisionMapper.selectPage(pageReqVO);
        List<AgentReplyReviewRespVO> list = pageResult.getList().stream()
                .map(this::buildRespVO)
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public Long approve(AgentReplyReviewApproveReqVO approveReqVO) {
        AgentReplyDecisionDO decision = validatePendingDecision(approveReqVO.getDecisionId());
        AgentMessageDO message = validatePendingMessage(decision.getSuggestedMessageId());
        AgentConversationDO conversation = validateConversationExists(decision.getConversationId());
        AgentWechatAccountDO account = validateCanSend(message.getWechatAccountId());
        AgentWechatContactDO contact = contactMapper.selectById(message.getContactId());

        String sendContent = StrUtil.blankToDefault(approveReqVO.getContent(), message.getContent());
        boolean edited = !Objects.equals(sendContent, message.getContent());
        GeweTextSendResult sendResult = geweMessageClient.sendText(account, contact.getExternalUserId(), sendContent);

        AgentMessageDO messageUpdate = new AgentMessageDO();
        messageUpdate.setId(message.getId());
        messageUpdate.setContent(sendContent);
        messageUpdate.setGeweMessageId(sendResult.geweMessageId());
        messageUpdate.setRawPayload(sendResult.rawResponse());
        messageUpdate.setSendStatus(sendResult.success() ? AgentConstants.SEND_STATUS_SENT : AgentConstants.SEND_STATUS_FAILED);
        messageUpdate.setAuditNote(resolveApproveAuditNote(edited, sendResult));
        messageUpdate.setOperatorUserId(getLoginUserIdQuietly());
        messageMapper.updateById(messageUpdate);

        if (!sendResult.success()) {
            throw exception(GEWE_SEND_FAILED);
        }

        AgentReplyDecisionDO decisionUpdate = new AgentReplyDecisionDO();
        decisionUpdate.setId(decision.getId());
        decisionUpdate.setSentMessageId(message.getId());
        decisionUpdate.setReviewStatus(edited ? AgentConstants.REVIEW_STATUS_EDITED : AgentConstants.REVIEW_STATUS_APPROVED);
        decisionUpdate.setReviewNote(edited ? "审核修改后发送" : "审核通过并发送");
        decisionUpdate.setReviewUserId(getLoginUserIdQuietly());
        decisionUpdate.setReviewTime(LocalDateTime.now());
        decisionMapper.updateById(decisionUpdate);

        updateConversationAfterSent(conversation, message.getId(), message.getMessageTime());
        return message.getId();
    }

    @Override
    public void reject(AgentReplyReviewRejectReqVO rejectReqVO) {
        AgentReplyDecisionDO decision = validatePendingDecision(rejectReqVO.getDecisionId());
        AgentMessageDO message = validatePendingMessage(decision.getSuggestedMessageId());

        AgentMessageDO messageUpdate = new AgentMessageDO();
        messageUpdate.setId(message.getId());
        messageUpdate.setSendStatus(AgentConstants.SEND_STATUS_REJECTED);
        messageUpdate.setAuditNote(StrUtil.maxLength("人工驳回：" + rejectReqVO.getReason(), 512));
        messageUpdate.setOperatorUserId(getLoginUserIdQuietly());
        messageMapper.updateById(messageUpdate);

        AgentReplyDecisionDO decisionUpdate = new AgentReplyDecisionDO();
        decisionUpdate.setId(decision.getId());
        decisionUpdate.setReviewStatus(AgentConstants.REVIEW_STATUS_REJECTED);
        decisionUpdate.setReviewNote(rejectReqVO.getReason());
        decisionUpdate.setReviewUserId(getLoginUserIdQuietly());
        decisionUpdate.setReviewTime(LocalDateTime.now());
        decisionMapper.updateById(decisionUpdate);
    }

    @Override
    public Long approveByMessage(AgentConversationMessageReviewReqVO reviewReqVO) {
        AgentReplyDecisionDO decision = validateDecisionByMessageId(reviewReqVO.getMessageId());
        AgentReplyReviewApproveReqVO approveReqVO = new AgentReplyReviewApproveReqVO();
        approveReqVO.setDecisionId(decision.getId());
        approveReqVO.setContent(reviewReqVO.getContent());
        return approve(approveReqVO);
    }

    @Override
    public void rejectByMessage(AgentConversationMessageReviewReqVO reviewReqVO) {
        AgentReplyDecisionDO decision = validateDecisionByMessageId(reviewReqVO.getMessageId());
        AgentReplyReviewRejectReqVO rejectReqVO = new AgentReplyReviewRejectReqVO();
        rejectReqVO.setDecisionId(decision.getId());
        rejectReqVO.setReason(StrUtil.blankToDefault(reviewReqVO.getReason(), "人工驳回"));
        reject(rejectReqVO);
    }

    private AgentReplyReviewRespVO buildRespVO(AgentReplyDecisionDO decision) {
        AgentReplyReviewRespVO respVO = BeanUtils.toBean(decision, AgentReplyReviewRespVO.class);
        fillGenerationInfo(respVO, decision.getKnowledgeRefs());
        respVO.setDecisionReason(toReadableDecisionReason(decision.getDecisionReason()));
        AgentConversationDO conversation = decision.getConversationId() == null
                ? null : conversationMapper.selectById(decision.getConversationId());
        if (conversation != null) {
            respVO.setContactId(conversation.getContactId());
        }
        if (decision.getSuggestedMessageId() == null) {
            return respVO;
        }
        AgentMessageDO message = messageMapper.selectById(decision.getSuggestedMessageId());
        if (message != null) {
            respVO.setSuggestedContent(message.getContent());
            respVO.setMatchedPolicy(message.getMatchedPolicy());
            respVO.setAuditNote(message.getAuditNote());
        }
        return respVO;
    }

    private String toReadableDecisionReason(String reason) {
        if (StrUtil.isBlank(reason)) {
            return reason;
        }
        if (StrUtil.equals(reason, "AI reply generator returned empty content")) {
            return "AI 没有生成可发送的回复，请人工查看客户问题后处理。常见原因：知识库未命中、知识库未配置，或模型返回为空。";
        }
        return reason;
    }

    private void fillGenerationInfo(AgentReplyReviewRespVO respVO, Map<String, Object> knowledgeRefs) {
        if (knowledgeRefs == null) {
            return;
        }
        respVO.setGenerationSource(string(knowledgeRefs.get("generationSource")));
        respVO.setLlmProvider(string(knowledgeRefs.get("llmProvider")));
        respVO.setActualLlmModel(string(knowledgeRefs.get("llmModel")));
    }

    private AgentReplyDecisionDO validatePendingDecision(Long id) {
        AgentReplyDecisionDO decision = decisionMapper.selectById(id);
        if (decision == null) {
            throw exception(REPLY_DECISION_NOT_EXISTS);
        }
        if (!Objects.equals(decision.getReviewStatus(), AgentConstants.REVIEW_STATUS_PENDING)) {
            throw exception(REPLY_DECISION_STATUS_INVALID);
        }
        return decision;
    }

    private AgentReplyDecisionDO validateDecisionByMessageId(Long messageId) {
        AgentReplyDecisionDO decision = decisionMapper.selectBySuggestedMessageId(messageId);
        if (decision == null) {
            throw exception(REPLY_DECISION_NOT_EXISTS);
        }
        return decision;
    }

    private AgentMessageDO validatePendingMessage(Long id) {
        AgentMessageDO message = messageMapper.selectById(id);
        if (message == null) {
            throw exception(MESSAGE_NOT_EXISTS);
        }
        if (!Objects.equals(message.getSendStatus(), AgentConstants.SEND_STATUS_PENDING_REVIEW)) {
            throw exception(MESSAGE_STATUS_INVALID);
        }
        return message;
    }

    private AgentConversationDO validateConversationExists(Long id) {
        AgentConversationDO conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            throw exception(CONVERSATION_NOT_EXISTS);
        }
        return conversation;
    }

    private AgentWechatAccountDO validateCanSend(Long accountId) {
        AgentWechatAccountDO account = accountMapper.selectById(accountId);
        if (!geweMessageClient.canSend(account)) {
            throw exception(GEWE_SEND_CONFIG_MISSING);
        }
        return account;
    }

    private String resolveApproveAuditNote(boolean edited, GeweTextSendResult sendResult) {
        if (!sendResult.success()) {
            return StrUtil.maxLength(sendResult.errorMessage(), 512);
        }
        return edited ? "审核修改后发送" : "审核通过并发送";
    }

    private void updateConversationAfterSent(AgentConversationDO conversation, Long messageId, LocalDateTime messageTime) {
        AgentConversationDO conversationUpdate = new AgentConversationDO();
        conversationUpdate.setId(conversation.getId());
        conversationUpdate.setStatus(AgentConstants.CONVERSATION_STATUS_AI_AUTO);
        conversationUpdate.setLastMessageId(messageId);
        conversationUpdate.setLastMessageTime(messageTime);
        conversationMapper.updateById(conversationUpdate);
    }

    private Long getLoginUserIdQuietly() {
        try {
            return SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
