package cn.ai.sales.module.agent.controller.admin.account;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentGeweCredentialRespVO;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentGeweCredentialSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentGeweCredentialDO;
import cn.ai.sales.module.agent.service.account.AgentGeweCredentialService;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 销冠 GeWe 凭证")
@RestController
@RequestMapping("/agent/gewe-credential")
@Validated
public class AgentGeweCredentialController {

    @Resource
    private AgentGeweCredentialService credentialService;

    @GetMapping("/list")
    @Operation(summary = "获得 GeWe 凭证列表")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:query')")
    public CommonResult<List<AgentGeweCredentialRespVO>> getCredentialList() {
        return success(credentialService.getCredentialList().stream().map(this::toRespVO).toList());
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "获得启用的 GeWe 凭证列表")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:query')")
    public CommonResult<List<AgentGeweCredentialRespVO>> getEnabledCredentialList() {
        return success(credentialService.getEnabledCredentialList().stream().map(this::toRespVO).toList());
    }

    @GetMapping("/get")
    @Operation(summary = "获得 GeWe 凭证")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:query')")
    public CommonResult<AgentGeweCredentialRespVO> getCredential(@RequestParam("id") Long id) {
        return success(toRespVO(credentialService.getCredential(id)));
    }

    @PostMapping("/save")
    @Operation(summary = "保存 GeWe 凭证")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:update')")
    public CommonResult<Long> saveCredential(@Valid @RequestBody AgentGeweCredentialSaveReqVO saveReqVO) {
        return success(credentialService.saveCredential(saveReqVO));
    }

    @GetMapping("/default")
    @Operation(summary = "获得默认 GeWe 凭证")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:query')")
    public CommonResult<AgentGeweCredentialRespVO> getDefaultCredential() {
        return success(toRespVO(credentialService.getDefaultCredential()));
    }

    @PostMapping("/save-default")
    @Operation(summary = "保存默认 GeWe 凭证")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:update')")
    public CommonResult<Long> saveDefaultCredential(@Valid @RequestBody AgentGeweCredentialSaveReqVO saveReqVO) {
        return success(credentialService.saveDefaultCredential(saveReqVO));
    }

    private AgentGeweCredentialRespVO toRespVO(AgentGeweCredentialDO credential) {
        if (credential == null) {
            return null;
        }
        AgentGeweCredentialRespVO respVO = BeanUtils.toBean(credential, AgentGeweCredentialRespVO.class);
        respVO.setGeweTokenConfigured(StrUtil.isNotBlank(credential.getGeweToken()));
        return respVO;
    }

}
