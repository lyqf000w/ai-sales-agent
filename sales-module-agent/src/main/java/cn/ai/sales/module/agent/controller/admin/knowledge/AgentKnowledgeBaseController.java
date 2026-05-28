package cn.ai.sales.module.agent.controller.admin.knowledge;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeBasePageReqVO;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeBaseRespVO;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeBaseSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeBaseDO;
import cn.ai.sales.module.agent.service.knowledge.AgentKnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 销冠知识库")
@RestController
@RequestMapping("/agent/knowledge-base")
@Validated
public class AgentKnowledgeBaseController {

    @Resource
    private AgentKnowledgeBaseService knowledgeBaseService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:create')")
    public CommonResult<Long> createKnowledgeBase(@Valid @RequestBody AgentKnowledgeBaseSaveReqVO createReqVO) {
        return success(knowledgeBaseService.createKnowledgeBase(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:update')")
    public CommonResult<Boolean> updateKnowledgeBase(@Valid @RequestBody AgentKnowledgeBaseSaveReqVO updateReqVO) {
        knowledgeBaseService.updateKnowledgeBase(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:delete')")
    public CommonResult<Boolean> deleteKnowledgeBase(@RequestParam("id") Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:query')")
    public CommonResult<AgentKnowledgeBaseRespVO> getKnowledgeBase(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(knowledgeBaseService.getKnowledgeBase(id), AgentKnowledgeBaseRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得知识库分页")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:query')")
    public CommonResult<PageResult<AgentKnowledgeBaseRespVO>> getKnowledgeBasePage(
            @Valid AgentKnowledgeBasePageReqVO pageReqVO) {
        PageResult<AgentKnowledgeBaseDO> pageResult = knowledgeBaseService.getKnowledgeBasePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AgentKnowledgeBaseRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用知识库列表")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:query')")
    public CommonResult<List<AgentKnowledgeBaseRespVO>> getSimpleKnowledgeBaseList() {
        return success(BeanUtils.toBean(knowledgeBaseService.getEnabledKnowledgeBaseList(),
                AgentKnowledgeBaseRespVO.class));
    }
}
