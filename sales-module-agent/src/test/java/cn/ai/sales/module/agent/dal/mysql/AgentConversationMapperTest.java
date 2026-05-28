package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.framework.tenant.core.context.TenantContextHolder;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class AgentConversationMapperTest {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(builderAssistant, AgentConversationDO.class);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void selectPageWithPendingReviewQueueFiltersWaitingConfirmConversations() {
        AgentConversationMapper mapper = mock(AgentConversationMapper.class, CALLS_REAL_METHODS);
        doReturn(PageResult.empty()).when(mapper)
                .selectPage(any(AgentConversationPageReqVO.class), any(Wrapper.class));
        AgentConversationPageReqVO reqVO = new AgentConversationPageReqVO();
        ReflectionTestUtils.setField(reqVO, "queueType", AgentConstants.CONVERSATION_QUEUE_PENDING_REVIEW);

        mapper.selectPage(reqVO);

        LambdaQueryWrapperX<AgentConversationDO> queryWrapper = captureQueryWrapper(mapper);
        assertThat(queryWrapper.getTargetSql()).contains("status = ?");
        assertThat(queryWrapper.getParamNameValuePairs())
                .containsValue(AgentConstants.CONVERSATION_STATUS_WAITING_CONFIRM);
    }

    @Test
    void selectPageWithRiskQueueFiltersOnlyRiskConversations() {
        AgentConversationMapper mapper = mock(AgentConversationMapper.class, CALLS_REAL_METHODS);
        doReturn(PageResult.empty()).when(mapper)
                .selectPage(any(AgentConversationPageReqVO.class), any(Wrapper.class));
        AgentConversationPageReqVO reqVO = new AgentConversationPageReqVO();
        ReflectionTestUtils.setField(reqVO, "queueType", AgentConstants.CONVERSATION_QUEUE_RISK);

        mapper.selectPage(reqVO);

        LambdaQueryWrapperX<AgentConversationDO> queryWrapper = captureQueryWrapper(mapper);
        assertThat(queryWrapper.getTargetSql()).contains("risk_level > ?");
        assertThat(queryWrapper.getTargetSql()).doesNotContain("status IN");
        assertThat(queryWrapper.getParamNameValuePairs())
                .containsValue(AgentConstants.RISK_LEVEL_GREEN);
    }

    @Test
    void selectPageWithTakeoverQueueFiltersHumanTakeoverConversations() {
        AgentConversationMapper mapper = mock(AgentConversationMapper.class, CALLS_REAL_METHODS);
        doReturn(PageResult.empty()).when(mapper)
                .selectPage(any(AgentConversationPageReqVO.class), any(Wrapper.class));
        AgentConversationPageReqVO reqVO = new AgentConversationPageReqVO();
        ReflectionTestUtils.setField(reqVO, "queueType", "TAKEOVER");

        mapper.selectPage(reqVO);

        LambdaQueryWrapperX<AgentConversationDO> queryWrapper = captureQueryWrapper(mapper);
        assertThat(queryWrapper.getTargetSql()).contains("status = ?");
        assertThat(queryWrapper.getParamNameValuePairs())
                .containsValue(AgentConstants.CONVERSATION_STATUS_HUMAN_TAKEOVER);
    }

    @Test
    void selectPageWithFollowUpPriorityQueueFiltersContactPriority() {
        assertFollowUpPriorityQueue(AgentConstants.CONVERSATION_QUEUE_FOCUS,
                AgentConstants.FOLLOW_UP_PRIORITY_FOCUS);
        assertFollowUpPriorityQueue(AgentConstants.CONVERSATION_QUEUE_URGENT,
                AgentConstants.FOLLOW_UP_PRIORITY_URGENT);
    }

    private void assertFollowUpPriorityQueue(String queueType, String followUpPriority) {
        TenantContextHolder.setTenantId(1L);
        AgentConversationMapper mapper = mock(AgentConversationMapper.class, CALLS_REAL_METHODS);
        doReturn(PageResult.empty()).when(mapper)
                .selectPage(any(AgentConversationPageReqVO.class), any(Wrapper.class));
        AgentConversationPageReqVO reqVO = new AgentConversationPageReqVO();
        ReflectionTestUtils.setField(reqVO, "queueType", queueType);

        mapper.selectPage(reqVO);

        LambdaQueryWrapperX<AgentConversationDO> queryWrapper = captureQueryWrapper(mapper);
        assertThat(queryWrapper.getTargetSql())
                .contains("contact_id IN (SELECT id FROM agent_wechat_contact")
                .contains("tenant_id = 1")
                .contains("follow_up_priority = '" + followUpPriority + "'");
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapperX<AgentConversationDO> captureQueryWrapper(AgentConversationMapper mapper) {
        ArgumentCaptor<Wrapper<AgentConversationDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        org.mockito.Mockito.verify(mapper).selectPage(any(AgentConversationPageReqVO.class), captor.capture());
        return (LambdaQueryWrapperX<AgentConversationDO>) captor.getValue();
    }

}
