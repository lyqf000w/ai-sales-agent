package cn.ai.sales.module.agent.service.gewe;

import java.util.Map;

public record GeweVoiceDownloadResult(boolean success,
                                      String fileUrl,
                                      String errorMessage,
                                      Map<String, Object> rawResponse) {

    public static GeweVoiceDownloadResult success(String fileUrl, Map<String, Object> rawResponse) {
        return new GeweVoiceDownloadResult(true, fileUrl, null, rawResponse);
    }

    public static GeweVoiceDownloadResult failure(String errorMessage, Map<String, Object> rawResponse) {
        return new GeweVoiceDownloadResult(false, null, errorMessage, rawResponse);
    }

}
