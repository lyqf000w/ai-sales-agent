package cn.ai.sales.module.agent.service.agent;

import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentPublishReqVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConfigVersionDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConfigVersionMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceImplTest {

    private final AgentMapper agentMapper = mock(AgentMapper.class);
    private final AgentConfigVersionMapper configVersionMapper = mock(AgentConfigVersionMapper.class);
    private final AgentServiceImpl service = newService();

    @Test
    void updateAgentCreatesNextDraftWhenCurrentDraftIsAlreadyPublished() {
        AgentDO existed = agent();
        existed.setDraftVersion(1);
        existed.setOnlineVersion(1);
        when(agentMapper.selectById(1L)).thenReturn(existed);

        AgentSaveReqVO reqVO = saveReq();
        service.updateAgent(reqVO);

        ArgumentCaptor<AgentDO> captor = ArgumentCaptor.forClass(AgentDO.class);
        verify(agentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getDraftVersion()).isEqualTo(2);
        assertThat(captor.getValue().getOnlineVersion()).isNull();
    }

    @Test
    void publishAgentSnapshotsDraftAndUpdatesOnlineVersion() {
        AgentDO existed = agent();
        existed.setDraftVersion(3);
        existed.setOnlineVersion(2);
        existed.setTone("专业、克制");
        existed.setWelcomeMessage("您好，我是销售助手");
        existed.setSystemPrompt("你是一名专业 B2B SaaS 销售顾问");
        existed.setLlmProvider("DEEPSEEK");
        existed.setLlmModel("deepseek-v4-pro");
        when(agentMapper.selectById(1L)).thenReturn(existed);

        AgentPublishReqVO reqVO = new AgentPublishReqVO();
        reqVO.setAgentId(1L);
        reqVO.setChangeSummary("更新报价口径");

        service.publishAgent(reqVO);

        ArgumentCaptor<AgentConfigVersionDO> versionCaptor = ArgumentCaptor.forClass(AgentConfigVersionDO.class);
        verify(configVersionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getAgentId()).isEqualTo(1L);
        assertThat(versionCaptor.getValue().getVersion()).isEqualTo(3);
        assertThat(versionCaptor.getValue().getChangeSummary()).isEqualTo("更新报价口径");
        assertThat(versionCaptor.getValue().getConfigSnapshot()).containsEntry("tone", "专业、克制");
        assertThat(versionCaptor.getValue().getConfigSnapshot())
                .containsEntry("systemPrompt", "你是一名专业 B2B SaaS 销售顾问")
                .containsEntry("llmProvider", "DEEPSEEK")
                .containsEntry("llmModel", "deepseek-v4-pro");

        ArgumentCaptor<AgentDO> agentCaptor = ArgumentCaptor.forClass(AgentDO.class);
        verify(agentMapper).updateById(agentCaptor.capture());
        assertThat(agentCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(agentCaptor.getValue().getOnlineVersion()).isEqualTo(3);
        assertThat(agentCaptor.getValue().getPublishedConfig()).containsEntry("welcomeMessage", "您好，我是销售助手");
    }

    @Test
    void getLlmModelOptionsReturnsDeepSeekV4ProAndFlash() {
        assertThat(service.getLlmModelOptions())
                .extracting("provider", "model", "label", "defaultModel")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("DEEPSEEK", "deepseek-v4-pro", "DeepSeek-V4-Pro", true),
                        org.assertj.core.groups.Tuple.tuple("DEEPSEEK", "deepseek-v4-flash", "DeepSeek-V4-Flash", false)
                );
    }

    @Test
    void updateAgentRejectsUnsupportedLlmModel() {
        AgentDO existed = agent();
        when(agentMapper.selectById(1L)).thenReturn(existed);

        AgentSaveReqVO reqVO = saveReq();
        reqVO.setLlmProvider("DEEPSEEK");
        reqVO.setLlmModel("deepseek-chat");

        assertThatThrownBy(() -> service.updateAgent(reqVO))
                .hasMessageContaining("Agent 大模型配置不支持");
    }

    private AgentServiceImpl newService() {
        AgentServiceImpl service = new AgentServiceImpl();
        ReflectionTestUtils.setField(service, "agentMapper", agentMapper);
        ReflectionTestUtils.setField(service, "configVersionMapper", configVersionMapper);
        return service;
    }

    private AgentSaveReqVO saveReq() {
        AgentSaveReqVO reqVO = new AgentSaveReqVO();
        reqVO.setId(1L);
        reqVO.setName("默认销售助手");
        reqVO.setReplyMode(AgentConstants.REPLY_MODE_MANUAL_CONFIRM);
        reqVO.setConfidenceThreshold(new BigDecimal("0.70"));
        reqVO.setMaxContinuousAutoReply(3);
        reqVO.setQuietMinutes(0);
        reqVO.setStatus(AgentConstants.STATUS_ENABLE);
        return reqVO;
    }

    private AgentDO agent() {
        AgentDO agent = new AgentDO();
        agent.setId(1L);
        agent.setName("默认销售助手");
        agent.setReplyMode(AgentConstants.REPLY_MODE_MANUAL_CONFIRM);
        agent.setConfidenceThreshold(new BigDecimal("0.70"));
        agent.setMaxContinuousAutoReply(3);
        agent.setQuietMinutes(0);
        agent.setStatus(AgentConstants.STATUS_ENABLE);
        return agent;
    }

}
