package cn.ai.sales.module.agent.service.gewe;

import java.util.Map;

public record GeweMediaDownloadResult(boolean success,
                                      String fileUrl,
                                      String errorMessage,
                                      Map<String, Object> rawResponse) {

    public static GeweMediaDownloadResult success(String fileUrl, Map<String, Object> rawResponse) {
        return new GeweMediaDownloadResult(true, fileUrl, null, rawResponse);
    }

    public static GeweMediaDownloadResult failure(String errorMessage, Map<String, Object> rawResponse) {
        return new GeweMediaDownloadResult(false, null, errorMessage, rawResponse);
    }

}
