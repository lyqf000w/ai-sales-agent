package cn.ai.sales.module.agent.controller.admin.sensitiverule;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.sensitiverule.vo.AgentSensitiveRulePageReqVO;
import cn.ai.sales.module.agent.controller.admin.sensitiverule.vo.AgentSensitiveRuleOptionsRespVO;
import cn.ai.sales.module.agent.controller.admin.sensitiverule.vo.AgentSensitiveRuleRespVO;
import cn.ai.sales.module.agent.controller.admin.sensitiverule.vo.AgentSensitiveRuleSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentSensitiveRuleDO;
import cn.ai.sales.module.agent.service.sensitiverule.AgentSensitiveRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 销冠人工升级规则")
@RestController
@RequestMapping("/agent/sensitive-rule")
@Validated
public class AgentSensitiveRuleController {

    @Resource
    private AgentSensitiveRuleService ruleService;

    @PostMapping("/create")
    @Operation(summary = "创建人工升级规则")
    @PreAuthorize("@ss.hasPermission('agent:sensitive-rule:create')")
    public CommonResult<Long> createRule(@Valid @RequestBody AgentSensitiveRuleSaveReqVO createReqVO) {
        return success(ruleService.createRule(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新人工升级规则")
    @PreAuthorize("@ss.hasPermission('agent:sensitive-rule:update')")
    public CommonResult<Boolean> updateRule(@Valid @RequestBody AgentSensitiveRuleSaveReqVO updateReqVO) {
        ruleService.updateRule(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除人工升级规则")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:sensitive-rule:delete')")
    public CommonResult<Boolean> deleteRule(@RequestParam("id") Long id) {
        ruleService.deleteRule(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得人工升级规则")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:sensitive-rule:query')")
    public CommonResult<AgentSensitiveRuleRespVO> getRule(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(ruleService.getRule(id), AgentSensitiveRuleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得人工升级规则分页")
    @PreAuthorize("@ss.hasPermission('agent:sensitive-rule:query')")
    public CommonResult<PageResult<AgentSensitiveRuleRespVO>> getRulePage(
            @Valid AgentSensitiveRulePageReqVO pageReqVO) {
        PageResult<AgentSensitiveRuleDO> pageResult = ruleService.getRulePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AgentSensitiveRuleRespVO.class));
    }

    @GetMapping("/options")
    @Operation(summary = "获得人工升级规则选项")
    @PreAuthorize("@ss.hasPermission('agent:sensitive-rule:query')")
    public CommonResult<AgentSensitiveRuleOptionsRespVO> getRuleOptions() {
        return success(ruleService.getRuleOptions());
    }

}
