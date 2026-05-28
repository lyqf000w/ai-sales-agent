package cn.ai.sales.module.agent.controller.open;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.tenant.core.aop.TenantIgnore;
import cn.ai.sales.module.agent.service.webhook.AgentWebhookService;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/api/v1/gewechat")
@Validated
public class GeweOpenCallbackController {

    @Resource
    private AgentWebhookService webhookService;

    @PostMapping("/callback")
    @PermitAll
    @TenantIgnore
    public CommonResult<Boolean> receiveCallback(@RequestParam("token") String token,
                                                 @RequestHeader(value = "X-GEWE-SIGNATURE", required = false)
                                                 String signature,
                                                 @RequestBody(required = false) Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return success(true);
        }
        webhookService.handleGeweCallback(token, payload == null ? Map.of() : payload, signature);
        return success(true);
    }

}
