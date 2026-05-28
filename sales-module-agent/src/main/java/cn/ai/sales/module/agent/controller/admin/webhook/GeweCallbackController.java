package cn.ai.sales.module.agent.controller.admin.webhook;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.tenant.core.aop.TenantIgnore;
import cn.ai.sales.module.agent.service.webhook.AgentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 销冠 Gewe 回调")
@RestController
@RequestMapping("/agent/gewe")
@Validated
public class GeweCallbackController {

    @Resource
    private AgentWebhookService webhookService;

    @PostMapping("/callback/{callbackToken}")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "接收 Gewe 微信消息回调")
    public CommonResult<Boolean> receiveCallback(@PathVariable("callbackToken") String callbackToken,
                                                 @RequestHeader(value = "X-GEWE-SIGNATURE", required = false)
                                                 String signature,
                                                 @RequestBody Map<String, Object> payload) {
        webhookService.handleGeweCallback(callbackToken, payload, signature);
        return success(true);
    }

}
