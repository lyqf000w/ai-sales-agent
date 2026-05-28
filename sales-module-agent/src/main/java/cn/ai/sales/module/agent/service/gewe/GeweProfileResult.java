package cn.ai.sales.module.agent.service.gewe;

import java.util.Map;

public record GeweProfileResult(
        String wxid,
        String nickName,
        String avatar,
        String mobile,
        Map<String, Object> rawResponse
) {
}
