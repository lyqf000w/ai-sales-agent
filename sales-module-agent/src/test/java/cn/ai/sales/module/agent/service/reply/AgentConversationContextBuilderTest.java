package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentContactTagDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentContactTagRelDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentContactTagMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentContactTagRelMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.knowledge.AgentKnowledgeRetrievalService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentConversationContextBuilderTest {

    private final AgentMessageMapper messageMapper = mock(AgentMessageMapper.class);
    private final AgentWechatContactMapper contactMapper = mock(AgentWechatContactMapper.class);
    private final AgentContactTagRelMapper tagRelMapper = mock(AgentContactTagRelMapper.class);
    private final AgentContactTagMapper tagMapper = mock(AgentContactTagMapper.class);
    private final AgentKnowledgeRetrievalService knowledgeRetrievalService = mock(AgentKnowledgeRetrievalService.class);
    private final AgentConversationContextBuilder builder = newBuilder();

    @Test
    void buildIncludesCustomerLevelAndTagNames() {
        AgentMessageDO inbound = new AgentMessageDO();
        inbound.setConversationId(10L);
        inbound.setContactId(20L);
        inbound.setContent("想了解企业报价");
        when(messageMapper.selectListByConversationId(10L)).thenReturn(List.of(inbound));
        when(knowledgeRetrievalService.retrieve(null, "想了解企业报价")).thenReturn(List.of());

        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setId(20L);
        contact.setCustomerLevel(AgentConstants.CUSTOMER_LEVEL_IMPORTANT);
        when(contactMapper.selectById(20L)).thenReturn(contact);

        AgentContactTagRelDO rel = new AgentContactTagRelDO();
        rel.setTagId(30L);
        when(tagRelMapper.selectListByContactId(20L)).thenReturn(List.of(rel));

        AgentContactTagDO tag = new AgentContactTagDO();
        tag.setId(30L);
        tag.setName("高意向");
        tag.setStatus(AgentConstants.STATUS_ENABLE);
        when(tagMapper.selectBatchIds(List.of(30L))).thenReturn(List.of(tag));

        AgentReplyContext context = builder.build(10L, inbound);

        assertThat(context.customerLevel()).isEqualTo(AgentConstants.CUSTOMER_LEVEL_IMPORTANT);
        assertThat(context.customerTagNames()).containsExactly("高意向");
    }

    private AgentConversationContextBuilder newBuilder() {
        AgentConversationContextBuilder builder = new AgentConversationContextBuilder();
        ReflectionTestUtils.setField(builder, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(builder, "contactMapper", contactMapper);
        ReflectionTestUtils.setField(builder, "tagRelMapper", tagRelMapper);
        ReflectionTestUtils.setField(builder, "tagMapper", tagMapper);
        ReflectionTestUtils.setField(builder, "knowledgeRetrievalService", knowledgeRetrievalService);
        return builder;
    }

}
