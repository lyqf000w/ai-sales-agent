package cn.ai.sales.module.agent.service.gewe;

import java.util.Map;

public record GeweTextSendResult(boolean success, String geweMessageId, String errorMessage,
                                 Map<String, Object> rawResponse) {

    public static GeweTextSendResult success(String geweMessageId, Map<String, Object> rawResponse) {
        return new GeweTextSendResult(true, geweMessageId, null, rawResponse);
    }

    public static GeweTextSendResult failure(String errorMessage, Map<String, Object> rawResponse) {
        return new GeweTextSendResult(false, null, errorMessage, rawResponse);
    }

}
