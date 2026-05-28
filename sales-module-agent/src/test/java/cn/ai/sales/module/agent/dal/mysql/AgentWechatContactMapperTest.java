package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.framework.tenant.core.context.TenantContextHolder;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactPageReqVO;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsDimensionRespVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class AgentWechatContactMapperTest {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(builderAssistant, AgentWechatContactDO.class);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void selectPageFiltersBySalesInsightFields() {
        AgentWechatContactMapper mapper = mock(AgentWechatContactMapper.class, CALLS_REAL_METHODS);
        doReturn(PageResult.empty()).when(mapper)
                .selectPage(any(AgentWechatContactPageReqVO.class), any(Wrapper.class));
        AgentWechatContactPageReqVO reqVO = new AgentWechatContactPageReqVO();
        ReflectionTestUtils.setField(reqVO, "purchaseIntention", "HIGH");
        ReflectionTestUtils.setField(reqVO, "salesStage", "QUOTE_NEGOTIATION");
        ReflectionTestUtils.setField(reqVO, "customerSentiment", "POSITIVE");
        ReflectionTestUtils.setField(reqVO, "followUpPriority", "URGENT");

        mapper.selectPage(reqVO);

        LambdaQueryWrapperX<AgentWechatContactDO> queryWrapper = captureQueryWrapper(mapper);
        assertThat(queryWrapper.getTargetSql())
                .contains("purchase_intention = ?")
                .contains("sales_stage = ?")
                .contains("customer_sentiment = ?")
                .contains("follow_up_priority = ?");
        assertThat(queryWrapper.getParamNameValuePairs())
                .containsValue("HIGH")
                .containsValue("QUOTE_NEGOTIATION")
                .containsValue("POSITIVE")
                .containsValue("URGENT");
    }

    @Test
    void selectConversationQueuePageFiltersPendingDecisionWithoutRequiringAllContactsToHaveConversation() {
        TenantContextHolder.setTenantId(1L);
        AgentWechatContactMapper mapper = mock(AgentWechatContactMapper.class, CALLS_REAL_METHODS);
        doReturn(PageResult.empty()).when(mapper)
                .selectPage(any(AgentWechatContactPageReqVO.class), any(Wrapper.class));
        AgentWechatContactPageReqVO reqVO = new AgentWechatContactPageReqVO();
        ReflectionTestUtils.setField(reqVO, "queueType", AgentConstants.CONVERSATION_QUEUE_PENDING_REVIEW);

        mapper.selectConversationQueuePage(reqVO);

        LambdaQueryWrapperX<AgentWechatContactDO> queryWrapper = captureQueryWrapper(mapper);
        assertThat(queryWrapper.getTargetSql())
                .contains("id IN (SELECT DISTINCT c.contact_id")
                .contains("agent_reply_decision d")
                .contains("agent_message m")
                .contains("d.review_status = 'PENDING'")
                .contains("m.send_status = 1")
                .contains("OR id IN (SELECT contact_id FROM agent_conversation")
                .contains("status = 2")
                .contains("d.tenant_id = 1");
    }

    @Test
    void selectConversationQueuePageExcludesLegacyChatroomMemberContacts() {
        TenantContextHolder.setTenantId(1L);
        AgentWechatContactMapper mapper = mock(AgentWechatContactMapper.class, CALLS_REAL_METHODS);
        doReturn(PageResult.empty()).when(mapper)
                .selectPage(any(AgentWechatContactPageReqVO.class), any(Wrapper.class));
        AgentWechatContactPageReqVO reqVO = new AgentWechatContactPageReqVO();

        mapper.selectConversationQueuePage(reqVO);

        LambdaQueryWrapperX<AgentWechatContactDO> queryWrapper = captureQueryWrapper(mapper);
        assertThat(queryWrapper.getTargetSql())
                .contains("NOT EXISTS (SELECT 1 FROM agent_message m")
                .contains("agent_wechat_contact.external_user_id NOT LIKE '%@chatroom'")
                .contains("CAST(m.raw_payload AS TEXT) LIKE '%@chatroom%'")
                .contains("COALESCE(CAST(m.raw_payload AS TEXT), '') LIKE '%group_msg_event%'")
                .contains("COALESCE(CAST(m.raw_payload AS TEXT), '') LIKE '%fromGroup%'")
                .contains("COALESCE(m.content, '') LIKE 'wxid\\_%:%'")
                .contains("NOT EXISTS (SELECT 1 FROM agent_message direct_m")
                .contains("direct_m.direction = 1")
                .contains("COALESCE(CAST(direct_m.raw_payload AS TEXT), '') NOT LIKE '%@chatroom%'")
                .contains("COALESCE(direct_m.content, '') NOT LIKE 'wxid\\_%:%'");
        assertThat(queryWrapper.getTargetSql()).doesNotContain("external_user_id NOT LIKE ?");
    }

    @Test
    void selectConversationQueuePageFiltersTakeoverAndPriorityQueues() {
        TenantContextHolder.setTenantId(1L);
        AgentWechatContactMapper mapper = mock(AgentWechatContactMapper.class, CALLS_REAL_METHODS);
        doReturn(PageResult.empty()).when(mapper)
                .selectPage(any(AgentWechatContactPageReqVO.class), any(Wrapper.class));

        AgentWechatContactPageReqVO takeoverReqVO = new AgentWechatContactPageReqVO();
        ReflectionTestUtils.setField(takeoverReqVO, "queueType", AgentConstants.CONVERSATION_QUEUE_TAKEOVER);
        mapper.selectConversationQueuePage(takeoverReqVO);
        LambdaQueryWrapperX<AgentWechatContactDO> takeoverQueryWrapper = captureQueryWrapper(mapper);
        assertThat(takeoverQueryWrapper.getTargetSql())
                .contains("status = 3")
                .contains("tenant_id = 1");

        AgentWechatContactMapper urgentMapper = mock(AgentWechatContactMapper.class, CALLS_REAL_METHODS);
        doReturn(PageResult.empty()).when(urgentMapper)
                .selectPage(any(AgentWechatContactPageReqVO.class), any(Wrapper.class));
        AgentWechatContactPageReqVO urgentReqVO = new AgentWechatContactPageReqVO();
        ReflectionTestUtils.setField(urgentReqVO, "queueType", AgentConstants.CONVERSATION_QUEUE_URGENT);
        urgentMapper.selectConversationQueuePage(urgentReqVO);
        LambdaQueryWrapperX<AgentWechatContactDO> urgentQueryWrapper = captureQueryWrapper(urgentMapper);
        assertThat(urgentQueryWrapper.getTargetSql()).contains("follow_up_priority = ?");
        assertThat(urgentQueryWrapper.getParamNameValuePairs())
                .containsValue(AgentConstants.FOLLOW_UP_PRIORITY_URGENT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void selectSalesInsightStatsGroupsContactInsightFields() {
        AgentWechatContactMapper mapper = mock(AgentWechatContactMapper.class, CALLS_REAL_METHODS);
        doReturn(List.of(Map.of("code", "HIGH", "count", 3L))).when(mapper).selectMaps(any(Wrapper.class));

        List<AgentStatisticsDimensionRespVO> stats = mapper.selectPurchaseIntentionStats();

        assertThat(stats).hasSize(1);
        assertThat(stats.get(0).getCode()).isEqualTo("HIGH");
        assertThat(stats.get(0).getCount()).isEqualTo(3L);

        ArgumentCaptor<Wrapper<AgentWechatContactDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        org.mockito.Mockito.verify(mapper).selectMaps(captor.capture());
        QueryWrapper<AgentWechatContactDO> queryWrapper = (QueryWrapper<AgentWechatContactDO>) captor.getValue();
        assertThat(queryWrapper.getSqlSelect())
                .contains("COALESCE(purchase_intention, 'UNKNOWN') AS code")
                .contains("COUNT(*) AS count");
        assertThat(captor.getValue().getTargetSql())
                .contains("GROUP BY purchase_intention");
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapperX<AgentWechatContactDO> captureQueryWrapper(AgentWechatContactMapper mapper) {
        ArgumentCaptor<Wrapper<AgentWechatContactDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        org.mockito.Mockito.verify(mapper).selectPage(any(AgentWechatContactPageReqVO.class), captor.capture());
        return (LambdaQueryWrapperX<AgentWechatContactDO>) captor.getValue();
    }

}
