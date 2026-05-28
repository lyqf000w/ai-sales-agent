package cn.ai.sales.module.agent.service.reply;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeepSeekAgentLlmClientTest {

    @Test
    void buildSystemPromptAsksForNaturalWechatReply() {
        DeepSeekAgentLlmClient client = new DeepSeekAgentLlmClient();
        AgentReplyContext context = new AgentReplyContext("你好", List.of(), List.of());

        String prompt = ReflectionTestUtils.invokeMethod(client, "buildSystemPrompt", context);

        assertThat(prompt)
                .contains("真人销售")
                .contains("微信消息")
                .contains("短句")
                .contains("先接住客户这句话")
                .contains("不要说“作为AI/系统/机器人”")
                .contains("不要写“感谢咨询”“如有其他问题请随时联系”");
    }

    @Test
    void buildUserPromptKeepsReplyShortByDefault() {
        DeepSeekAgentLlmClient client = new DeepSeekAgentLlmClient();
        AgentReplyContext context = new AgentReplyContext("你好", List.of(), List.of());

        String prompt = ReflectionTestUtils.invokeMethod(client, "buildUserPrompt", context);

        assertThat(prompt)
                .contains("1 到 3 句话")
                .contains("日常微信口吻")
                .contains("不要用编号列表");
    }
}
