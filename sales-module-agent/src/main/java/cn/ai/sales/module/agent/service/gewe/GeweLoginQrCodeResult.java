package cn.ai.sales.module.agent.service.gewe;

import java.util.Map;

public record GeweLoginQrCodeResult(
        String appId,
        String uuid,
        String qrData,
        String qrImgBase64,
        Map<String, Object> rawResponse
) {
}
