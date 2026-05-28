package cn.ai.sales.module.agent.controller.admin.agent;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentConfigVersionRespVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentLlmModelOptionRespVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentPageReqVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentPublishReqVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentRespVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentSaveReqVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentSimpleRespVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentDO;
import cn.ai.sales.module.agent.service.agent.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 销冠 Agent")
@RestController
@RequestMapping("/agent/agent")
@Validated
public class AgentController {

    @Resource
    private AgentService agentService;

    @PostMapping("/create")
    @Operation(summary = "创建 Agent")
    @PreAuthorize("@ss.hasPermission('agent:agent:create')")
    public CommonResult<Long> createAgent(@Valid @RequestBody AgentSaveReqVO createReqVO) {
        return success(agentService.createAgent(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 Agent")
    @PreAuthorize("@ss.hasPermission('agent:agent:update')")
    public CommonResult<Boolean> updateAgent(@Valid @RequestBody AgentSaveReqVO updateReqVO) {
        agentService.updateAgent(updateReqVO);
        return success(true);
    }

    @PostMapping("/publish")
    @Operation(summary = "发布 Agent 配置")
    @PreAuthorize("@ss.hasPermission('agent:agent:publish')")
    public CommonResult<Boolean> publishAgent(@Valid @RequestBody AgentPublishReqVO publishReqVO) {
        agentService.publishAgent(publishReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 Agent")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:agent:delete')")
    public CommonResult<Boolean> deleteAgent(@RequestParam("id") Long id) {
        agentService.deleteAgent(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 Agent")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:agent:query')")
    public CommonResult<AgentRespVO> getAgent(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(agentService.getAgent(id), AgentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 Agent 分页")
    @PreAuthorize("@ss.hasPermission('agent:agent:query')")
    public CommonResult<PageResult<AgentRespVO>> getAgentPage(@Valid AgentPageReqVO pageReqVO) {
        PageResult<AgentDO> pageResult = agentService.getAgentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AgentRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得可用 Agent 列表")
    @PreAuthorize("@ss.hasPermission('agent:agent:query')")
    public CommonResult<List<AgentSimpleRespVO>> getSimpleAgentList() {
        return success(BeanUtils.toBean(agentService.getEnabledAgents(), AgentSimpleRespVO.class));
    }

    @GetMapping("/llm-model-options")
    @Operation(summary = "获得大模型选项")
    @PreAuthorize("@ss.hasPermission('agent:agent:query')")
    public CommonResult<List<AgentLlmModelOptionRespVO>> getLlmModelOptions() {
        return success(agentService.getLlmModelOptions());
    }

    @GetMapping("/versions")
    @Operation(summary = "获得 Agent 配置版本列表")
    @Parameter(name = "agentId", description = "Agent 编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:agent:query')")
    public CommonResult<List<AgentConfigVersionRespVO>> getConfigVersions(@RequestParam("agentId") Long agentId) {
        return success(BeanUtils.toBean(agentService.getConfigVersions(agentId), AgentConfigVersionRespVO.class));
    }

}
