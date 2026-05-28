package cn.ai.sales.module.agent.service.knowledge;

import cn.ai.sales.framework.common.util.http.HttpUtils;
import cn.ai.sales.framework.common.util.json.JsonUtils;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class OpenAiCompatibleAgentEmbeddingClient implements AgentEmbeddingClient {

    @Resource
    private AgentEmbeddingProperties properties;

    @Override
    public boolean isEnabled() {
        return StrUtil.isAllNotBlank(properties.getUrl(), properties.getModel());
    }

    @Override
    public List<Double> embed(String text) {
        if (!isEnabled() || StrUtil.isBlank(text)) {
            return List.of();
        }
        Map<String, String> headers = new HashMap<>();
        if (StrUtil.isNotBlank(properties.getApiKey())) {
            headers.put("Authorization", "Bearer " + properties.getApiKey());
        }
        headers.put("Content-Type", "application/json");
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getModel());
        body.put("input", text);
        try {
            String responseBody = HttpUtils.post(properties.getUrl(), headers, JsonUtils.toJsonString(body));
            Map<String, Object> response = JsonUtils.getObjectMapper().readValue(responseBody, new TypeReference<>() {
            });
            return readFirstEmbedding(response);
        } catch (Exception ex) {
            log.warn("[embed][model({}) failed]", properties.getModel(), ex);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Double> readFirstEmbedding(Map<String, Object> response) {
        Object data = response.get("data");
        if (!(data instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> item)) {
            return List.of();
        }
        Object embedding = ((Map<String, Object>) item).get("embedding");
        if (!(embedding instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value instanceof Number)
                .map(value -> ((Number) value).doubleValue())
                .toList();
    }
}
