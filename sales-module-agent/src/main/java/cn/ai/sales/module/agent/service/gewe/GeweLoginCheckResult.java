package cn.ai.sales.module.agent.service.gewe;

import java.util.Map;

public record GeweLoginCheckResult(
        boolean success,
        boolean waitConfirm,
        String verifyUrl,
        String nickName,
        String avatar,
        Map<String, Object> rawResponse
) {
}
