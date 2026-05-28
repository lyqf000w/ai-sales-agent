package cn.ai.sales.module.agent.controller.admin.account;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatAccountPageReqVO;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatAccountRespVO;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatAccountSaveReqVO;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatBindSessionCreateReqVO;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatBindSessionRespVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentGeweCredentialDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatBindSessionDO;
import cn.ai.sales.module.agent.service.account.AgentGeweCredentialService;
import cn.ai.sales.module.agent.service.account.AgentWechatBindSessionService;
import cn.ai.sales.module.agent.service.account.AgentWechatAccountService;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 销冠微信账号")
@RestController
@RequestMapping("/agent/wechat-account")
@Validated
public class AgentWechatAccountController {

    @Resource
    private AgentWechatAccountService wechatAccountService;
    @Resource
    private AgentWechatBindSessionService bindSessionService;
    @Resource
    private AgentGeweCredentialService credentialService;

    @PostMapping("/create")
    @Operation(summary = "创建微信账号")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:create')")
    public CommonResult<Long> createWechatAccount(@Valid @RequestBody AgentWechatAccountSaveReqVO createReqVO) {
        return success(wechatAccountService.createWechatAccount(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新微信账号")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:update')")
    public CommonResult<Boolean> updateWechatAccount(@Valid @RequestBody AgentWechatAccountSaveReqVO updateReqVO) {
        wechatAccountService.updateWechatAccount(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除微信账号")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:delete')")
    public CommonResult<Boolean> deleteWechatAccount(@RequestParam("id") Long id) {
        wechatAccountService.deleteWechatAccount(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得微信账号")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:query')")
    public CommonResult<AgentWechatAccountRespVO> getWechatAccount(@RequestParam("id") Long id) {
        AgentWechatAccountDO account = wechatAccountService.getWechatAccount(id);
        return success(toRespVO(account));
    }

    @GetMapping("/page")
    @Operation(summary = "获得微信账号分页")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:query')")
    public CommonResult<PageResult<AgentWechatAccountRespVO>> getWechatAccountPage(
            @Valid AgentWechatAccountPageReqVO pageReqVO) {
        PageResult<AgentWechatAccountDO> pageResult = wechatAccountService.getWechatAccountPage(pageReqVO);
        return success(new PageResult<>(pageResult.getList().stream().map(this::toRespVO).toList(),
                pageResult.getTotal()));
    }

    @PostMapping("/bind-session/create")
    @Operation(summary = "创建微信扫码绑定会话")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:create')")
    public CommonResult<AgentWechatBindSessionRespVO> createBindSession(
            @Valid @RequestBody AgentWechatBindSessionCreateReqVO createReqVO) {
        Long id = bindSessionService.createBindSession(createReqVO);
        return success(toBindSessionRespVO(bindSessionService.getBindSession(id)));
    }

    @PostMapping("/login/start")
    @Operation(summary = "Start WeChat QR login")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:create')")
    public CommonResult<AgentWechatBindSessionRespVO> startLogin(
            @Valid @RequestBody AgentWechatBindSessionCreateReqVO createReqVO) {
        Long id = bindSessionService.createBindSession(createReqVO);
        return success(toBindSessionRespVO(bindSessionService.getBindSession(id)));
    }

    @GetMapping("/bind-session/get")
    @Operation(summary = "获得微信扫码绑定会话")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:query')")
    public CommonResult<AgentWechatBindSessionRespVO> getBindSession(@RequestParam("id") Long id) {
        return success(toBindSessionRespVO(bindSessionService.getBindSession(id)));
    }

    @PostMapping("/bind-session/check")
    @Operation(summary = "检查微信扫码绑定会话")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:create')")
    public CommonResult<AgentWechatBindSessionRespVO> checkBindSession(@RequestParam("id") Long id) {
        return success(toBindSessionRespVO(bindSessionService.checkBindSession(id)));
    }

    @GetMapping("/login/status")
    @Operation(summary = "Get WeChat QR login status")
    @Parameter(name = "id", description = "Login session id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:wechat-account:query')")
    public CommonResult<AgentWechatBindSessionRespVO> getLoginStatus(@RequestParam("id") Long id) {
        return success(toBindSessionRespVO(bindSessionService.checkBindSession(id)));
    }

    private AgentWechatAccountRespVO toRespVO(AgentWechatAccountDO account) {
        if (account == null) {
            return null;
        }
        AgentWechatAccountRespVO respVO = BeanUtils.toBean(account, AgentWechatAccountRespVO.class);
        AgentGeweCredentialDO credential = credentialService.getCredential(account.getGeweCredentialId());
        if (credential != null) {
            respVO.setGeweCredentialName(credential.getName());
            respVO.setCallbackUrl(StrUtil.blankToDefault(credential.getCallbackUrl(), account.getCallbackUrl()));
            respVO.setGeweApiBaseUrl(StrUtil.blankToDefault(credential.getGeweApiBaseUrl(), account.getGeweApiBaseUrl()));
            respVO.setGeweTokenConfigured(StrUtil.isNotBlank(credential.getGeweToken()));
        } else {
            respVO.setGeweTokenConfigured(StrUtil.isNotBlank(account.getGeweToken()));
        }
        return respVO;
    }

    private AgentWechatBindSessionRespVO toBindSessionRespVO(AgentWechatBindSessionDO session) {
        return BeanUtils.toBean(session, AgentWechatBindSessionRespVO.class);
    }

}
