package cn.ai.sales.module.agent.service.risk;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.security.core.util.SecurityFrameworkUtils;
import cn.ai.sales.module.agent.controller.admin.risk.vo.AgentRiskPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.CONVERSATION_NOT_EXISTS;

@Service
@Validated
public class AgentRiskServiceImpl implements AgentRiskService {

    @Resource
    private AgentConversationMapper conversationMapper;
    @Resource
    private AgentMessageMapper messageMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;

    @Override
    public PageResult<AgentConversationDO> getRiskPage(AgentRiskPageReqVO pageReqVO) {
        return conversationMapper.selectRiskPage(pageReqVO);
    }

    @Override
    public List<AgentMessageDO> getRiskMessages(Long conversationId) {
        validateConversationExists(conversationId);
        return messageMapper.selectListByConversationId(conversationId);
    }

    @Override
    public void takeover(Long conversationId) {
        AgentConversationDO conversation = validateConversationExists(conversationId);
        LocalDateTime now = LocalDateTime.now();

        AgentConversationDO conversationUpdate = new AgentConversationDO();
        conversationUpdate.setId(conversation.getId());
        conversationUpdate.setStatus(AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
        conversationUpdate.setRiskLevel(AgentConstants.RISK_LEVEL_RED);
        conversationUpdate.setContinuousAutoReplyCount(0);
        conversationUpdate.setHumanTakeoverUserId(getLoginUserIdQuietly());
        conversationUpdate.setHumanTakeoverTime(now);
        conversationMapper.updateById(conversationUpdate);

        updateContactStatus(conversation.getContactId(), AgentConstants.RISK_LEVEL_RED,
                AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
    }

    @Override
    public void close(Long conversationId) {
        AgentConversationDO conversation = validateConversationExists(conversationId);

        AgentConversationDO conversationUpdate = new AgentConversationDO();
        conversationUpdate.setId(conversation.getId());
        conversationUpdate.setStatus(AgentConstants.CONVERSATION_STATUS_CLOSED);
        conversationUpdate.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        conversationUpdate.setContinuousAutoReplyCount(0);
        conversationMapper.updateById(conversationUpdate);

        updateContactStatus(conversation.getContactId(), AgentConstants.RISK_LEVEL_GREEN,
                AgentConstants.CONVERSATION_STATUS_CLOSED);
    }

    private AgentConversationDO validateConversationExists(Long id) {
        AgentConversationDO conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            throw exception(CONVERSATION_NOT_EXISTS);
        }
        return conversation;
    }

    private void updateContactStatus(Long contactId, Integer riskLevel, Integer conversationStatus) {
        AgentWechatContactDO contactUpdate = new AgentWechatContactDO();
        contactUpdate.setId(contactId);
        contactUpdate.setRiskLevel(riskLevel);
        contactUpdate.setLastConversationStatus(conversationStatus);
        contactMapper.updateById(contactUpdate);
    }

    private Long getLoginUserIdQuietly() {
        try {
            return SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

}
