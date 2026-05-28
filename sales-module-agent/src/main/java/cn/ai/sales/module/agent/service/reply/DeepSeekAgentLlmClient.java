package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.framework.common.util.http.HttpUtils;
import cn.ai.sales.framework.common.util.json.JsonUtils;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.knowledge.AgentKnowledgeHit;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class DeepSeekAgentLlmClient implements AgentLlmClient {

    @Resource
    private AgentDeepSeekLlmProperties properties;

    @Override
    public boolean supports(String provider, String model) {
        return StrUtil.isBlank(provider)
                || StrUtil.equalsIgnoreCase(provider, "DEEPSEEK")
                || StrUtil.equalsIgnoreCase(provider, "DeepSeek");
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getEnabled())
                && StrUtil.isAllNotBlank(properties.getUrl(), properties.getApiKey(), properties.getModel());
    }

    @Override
    public AgentGeneratedReply generate(AgentReplyContext context) {
        if (!isEnabled() || context == null || StrUtil.isBlank(context.inboundContent())) {
            return AgentGeneratedReply.blank();
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + properties.getApiKey());
        headers.put("Content-Type", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("model", StrUtil.blankToDefault(context.llmModel(), properties.getModel()));
        body.put("temperature", properties.getTemperature());
        body.put("max_tokens", properties.getMaxTokens());
        body.put("stream", false);
        body.put("thinking", Map.of("type", Boolean.TRUE.equals(properties.getThinkingEnabled())
                ? "enabled" : "disabled"));
        body.put("messages", buildMessages(context));
        try {
            String responseBody = HttpUtils.post(properties.getUrl(), headers, JsonUtils.toJsonString(body));
            Map<String, Object> response = JsonUtils.getObjectMapper().readValue(responseBody, new TypeReference<>() {
            });
            String content = AgentReplyTonePolisher.polish(readReplyContent(response));
            if (StrUtil.isBlank(content)) {
                log.warn("[generate][deepseek empty response, model({})]", body.get("model"));
                return AgentGeneratedReply.blank();
            }
            return AgentGeneratedReply.llm(content, firstKnowledgeTitle(context.knowledgeHits()),
                    new BigDecimal("0.85"), "DEEPSEEK", Objects.toString(body.get("model"), null));
        } catch (Exception ex) {
            log.warn("[generate][deepseek failed, model({})]", body.get("model"), ex);
            return AgentGeneratedReply.blank();
        }
    }

    private List<Map<String, String>> buildMessages(AgentReplyContext context) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", buildSystemPrompt(context)));
        String history = buildRecentHistory(context.recentMessages());
        if (StrUtil.isNotBlank(history)) {
            messages.add(message("user", "最近对话：\n" + history));
        }
        messages.add(message("user", buildUserPrompt(context)));
        return messages;
    }

    private String buildSystemPrompt(AgentReplyContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个正在微信里跟客户聊天的真人销售。请直接给出可以发给客户的中文回复。");
        prompt.append("要求：优先依据知识库内容；不要编造价格、合同、交付、法务、医疗或金融承诺；");
        prompt.append("信息不足时说需要再确认。回复要像真人微信消息：短句、口语、自然、有分寸；");
        prompt.append("先接住客户这句话，再给结论或下一步，不要一上来就像客服手册。");
        prompt.append("可以用“可以的”“我看下”“这个我帮您确认下”等自然说法，但不要油腻、不要夸张。");
        prompt.append("不要说“作为AI/系统/机器人”，不要写分析过程，不要一上来就长篇解释，");
        prompt.append("不要写“感谢咨询”“如有其他问题请随时联系”这类模板结束语。");
        if (StrUtil.isNotBlank(context.systemPrompt())) {
            prompt.append("\n业务提示词：").append(context.systemPrompt());
        }
        if (context.customerLevel() != null) {
            prompt.append("\n客户等级：").append(context.customerLevel());
        }
        if (CollUtil.isNotEmpty(context.customerTagNames())) {
            prompt.append("\n客户标签：").append(String.join("、", context.customerTagNames()));
        }
        return prompt.toString();
    }

    private String buildUserPrompt(AgentReplyContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("客户消息：").append(context.inboundContent()).append("\n");
        if (CollUtil.isNotEmpty(context.knowledgeHits())) {
            prompt.append("\n可引用知识库片段：\n");
            for (int i = 0; i < Math.min(context.knowledgeHits().size(), 5); i++) {
                AgentKnowledgeHit hit = context.knowledgeHits().get(i);
                if (hit == null || !hit.hasContent()) {
                    continue;
                }
                prompt.append(i + 1).append(". ");
                prompt.append(StrUtil.blankToDefault(hit.title(), "知识条目"));
                prompt.append("：").append(StrUtil.maxLength(hit.content(), 700)).append("\n");
            }
        } else if (context.knowledgeBaseId() != null) {
            prompt.append("\n当前知识库未检索到可引用内容，请谨慎回复，并提示需要进一步确认。");
        }
        prompt.append("\n只输出要发送给客户的正文。一般 1 到 3 句话、每句尽量短；");
        prompt.append("优先用日常微信口吻，不要用编号列表，除非客户明确要清单。");
        return prompt.toString();
    }

    private String buildRecentHistory(List<AgentMessageDO> recentMessages) {
        if (CollUtil.isEmpty(recentMessages)) {
            return "";
        }
        StringBuilder history = new StringBuilder();
        int start = Math.max(0, recentMessages.size() - 6);
        for (int i = start; i < recentMessages.size(); i++) {
            AgentMessageDO message = recentMessages.get(i);
            if (message == null || StrUtil.isBlank(message.getContent())) {
                continue;
            }
            String role = Objects.equals(message.getDirection(), AgentConstants.MESSAGE_DIRECTION_INBOUND)
                    ? "客户" : "销售";
            history.append(role).append("：")
                    .append(StrUtil.maxLength(message.getContent(), 300))
                    .append("\n");
        }
        return history.toString();
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    @SuppressWarnings("unchecked")
    private String readReplyContent(Map<String, Object> response) {
        Object choices = response.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> choice)) {
            return "";
        }
        Object message = ((Map<String, Object>) choice).get("message");
        if (!(message instanceof Map<?, ?> messageMap)) {
            return "";
        }
        return Objects.toString(((Map<String, Object>) messageMap).get("content"), "");
    }

    private String firstKnowledgeTitle(List<AgentKnowledgeHit> knowledgeHits) {
        if (CollUtil.isEmpty(knowledgeHits)) {
            return null;
        }
        return knowledgeHits.stream()
                .filter(Objects::nonNull)
                .map(AgentKnowledgeHit::title)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
    }

}
