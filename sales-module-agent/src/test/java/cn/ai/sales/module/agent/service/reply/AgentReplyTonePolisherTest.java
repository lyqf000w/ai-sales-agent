package cn.ai.sales.module.agent.service.reply;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentReplyTonePolisherTest {

    @Test
    void polishRemovesRoboticGreetingAndClosing() {
        String reply = AgentReplyTonePolisher.polish(
                "您好，感谢您的咨询。企业版需要结合席位数报价。如有其他问题，请随时联系我们。");

        assertThat(reply).isEqualTo("企业版需要结合席位数报价。");
    }

    @Test
    void polishRemovesAiIdentityAndKnowledgePrefix() {
        String reply = AgentReplyTonePolisher.polish(
                "我是AI销售助手。根据知识库显示，退款需要先核对订单号。");

        assertThat(reply).isEqualTo("退款需要先核对订单号。");
    }

    @Test
    void polishKeepsUsefulNaturalReply() {
        String reply = AgentReplyTonePolisher.polish("可以的，我先帮您看下订单状态。");

        assertThat(reply).isEqualTo("可以的，我先帮您看下订单状态。");
    }

}
