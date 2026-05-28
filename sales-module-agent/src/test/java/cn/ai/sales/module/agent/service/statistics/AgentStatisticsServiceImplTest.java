package cn.ai.sales.module.agent.service.statistics;

import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsDimensionRespVO;
import cn.ai.sales.module.agent.controller.admin.statistics.vo.AgentStatisticsSummaryRespVO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentReplyDecisionMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentStatisticsServiceImplTest {

    private final AgentMessageMapper messageMapper = mock(AgentMessageMapper.class);
    private final AgentReplyDecisionMapper replyDecisionMapper = mock(AgentReplyDecisionMapper.class);
    private final AgentConversationMapper conversationMapper = mock(AgentConversationMapper.class);
    private final AgentWechatContactMapper contactMapper = mock(AgentWechatContactMapper.class);
    private final AgentStatisticsServiceImpl service = newService();

    @Test
    void getSummaryIncludesSalesInsightDimensions() {
        when(messageMapper.selectCountByMessageTimeBetween(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(8L);
        when(messageMapper.selectAutoReplyCountByMessageTimeBetween(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(3L);
        when(replyDecisionMapper.selectPendingReviewCount()).thenReturn(2L);
        when(conversationMapper.selectRiskCount()).thenReturn(1L);
        when(contactMapper.selectPurchaseIntentionStats()).thenReturn(List.of(stat("HIGH", 4L)));
        when(contactMapper.selectSalesStageStats()).thenReturn(List.of(stat("QUOTE_NEGOTIATION", 3L)));
        when(contactMapper.selectCustomerSentimentStats()).thenReturn(List.of(stat("POSITIVE", 5L)));
        when(contactMapper.selectFollowUpPriorityStats()).thenReturn(List.of(stat("URGENT", 2L)));

        AgentStatisticsSummaryRespVO summary = service.getSummary();

        assertThat(summary.getTodayMessageCount()).isEqualTo(8L);
        assertThat(summary.getPurchaseIntentionStats()).extracting(AgentStatisticsDimensionRespVO::getCode)
                .containsExactly("HIGH");
        assertThat(summary.getSalesStageStats()).extracting(AgentStatisticsDimensionRespVO::getCode)
                .containsExactly("QUOTE_NEGOTIATION");
        assertThat(summary.getCustomerSentimentStats()).extracting(AgentStatisticsDimensionRespVO::getCode)
                .containsExactly("POSITIVE");
        assertThat(summary.getFollowUpPriorityStats()).extracting(AgentStatisticsDimensionRespVO::getCode)
                .containsExactly("URGENT");
    }

    private AgentStatisticsServiceImpl newService() {
        AgentStatisticsServiceImpl service = new AgentStatisticsServiceImpl();
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "replyDecisionMapper", replyDecisionMapper);
        ReflectionTestUtils.setField(service, "conversationMapper", conversationMapper);
        ReflectionTestUtils.setField(service, "contactMapper", contactMapper);
        return service;
    }

    private AgentStatisticsDimensionRespVO stat(String code, Long count) {
        AgentStatisticsDimensionRespVO respVO = new AgentStatisticsDimensionRespVO();
        respVO.setCode(code);
        respVO.setCount(count);
        return respVO;
    }

}
