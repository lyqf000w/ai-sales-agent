package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAgentIntentRecognitionServiceTest {

    private final DefaultAgentIntentRecognitionService service = new DefaultAgentIntentRecognitionService();

    @Test
    void recognizeKnowledgeGapIntentWhenCustomerQuestionsKnowledgeAnswering() {
        AgentIntentAnalysis analysis = service.recognize(null, null,
                message("如果知识库找不到答案，会不会乱答？"));

        assertThat(analysis.intent()).isEqualTo("knowledge_gap");
        assertThat(analysis.needsHuman()).isTrue();
        assertThat(analysis.riskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_YELLOW);
    }

    @Test
    void recognizeKeyCustomerProjectIntentWhenCustomerMentionsLargeProject() {
        AgentIntentAnalysis analysis = service.recognize(null, null,
                message("我们是大客户项目，采购流程已经立项，想推进一下方案。"));

        assertThat(analysis.intent()).isEqualTo("key_customer_project");
        assertThat(analysis.needsHuman()).isTrue();
        assertThat(analysis.riskLevel()).isEqualTo(AgentConstants.RISK_LEVEL_YELLOW);
    }

    private AgentMessageDO message(String content) {
        AgentMessageDO message = new AgentMessageDO();
        message.setContent(content);
        return message;
    }

}
