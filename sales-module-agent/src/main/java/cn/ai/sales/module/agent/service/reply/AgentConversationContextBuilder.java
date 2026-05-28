package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentContactTagDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentContactTagRelDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentContactTagMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentContactTagRelMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.knowledge.AgentKnowledgeRetrievalService;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class AgentConversationContextBuilder {

    @Resource
    private AgentMessageMapper messageMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private AgentContactTagRelMapper tagRelMapper;
    @Resource
    private AgentContactTagMapper tagMapper;
    @Resource
    private AgentKnowledgeRetrievalService knowledgeRetrievalService;

    public AgentReplyContext build(Long conversationId, AgentMessageDO inboundMessage) {
        return build(conversationId, inboundMessage, null);
    }

    public AgentReplyContext build(Long conversationId, AgentMessageDO inboundMessage, AgentDO agent) {
        return build(conversationId, inboundMessage, agent, null);
    }

    public AgentReplyContext build(Long conversationId, AgentMessageDO inboundMessage, AgentDO agent,
                                   AgentWechatAccountDO account) {
        List<AgentMessageDO> recentMessages = messageMapper.selectListByConversationId(conversationId);
        AgentWechatContactDO contact = inboundMessage.getContactId() == null ? null
                : contactMapper.selectById(inboundMessage.getContactId());
        Long knowledgeBaseId = resolveKnowledgeBaseId(agent, account);
        return new AgentReplyContext(inboundMessage.getContent(), recentMessages,
                knowledgeRetrievalService.retrieve(knowledgeBaseId, inboundMessage.getContent()),
                inboundMessage.getContactId(), resolveCustomerLevel(contact), resolveTagNames(inboundMessage.getContactId()),
                agent == null ? null : agent.getId(), knowledgeBaseId,
                agent == null ? null : agent.getSystemPrompt(),
                agent == null ? null : agent.getLlmProvider(),
                agent == null ? null : agent.getLlmModel());
    }

    private Long resolveKnowledgeBaseId(AgentDO agent, AgentWechatAccountDO account) {
        if (account != null && account.getKnowledgeBaseId() != null) {
            return account.getKnowledgeBaseId();
        }
        return agent == null ? null : agent.getKnowledgeBaseId();
    }

    private Integer resolveCustomerLevel(AgentWechatContactDO contact) {
        return contact == null || contact.getCustomerLevel() == null
                ? AgentConstants.CUSTOMER_LEVEL_NORMAL : contact.getCustomerLevel();
    }

    private List<String> resolveTagNames(Long contactId) {
        if (contactId == null) {
            return List.of();
        }
        List<Long> tagIds = tagRelMapper.selectListByContactId(contactId).stream()
                .map(AgentContactTagRelDO::getTagId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (tagIds.isEmpty()) {
            return List.of();
        }
        List<AgentContactTagDO> tags = tagMapper.selectBatchIds(tagIds);
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (AgentContactTagDO tag : tags) {
            if (tag != null && Objects.equals(tag.getStatus(), AgentConstants.STATUS_ENABLE)
                    && StrUtil.isNotBlank(tag.getName())) {
                names.add(tag.getName());
            }
        }
        return names;
    }

}
