package cn.ai.sales.module.agent.controller.admin.knowledge;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeItemPageReqVO;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeItemRespVO;
import cn.ai.sales.module.agent.controller.admin.knowledge.vo.AgentKnowledgeItemSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeItemDO;
import cn.ai.sales.module.agent.service.knowledge.AgentKnowledgeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 销冠知识库")
@RestController
@RequestMapping("/agent/knowledge")
@Validated
public class AgentKnowledgeItemController {

    @Resource
    private AgentKnowledgeItemService knowledgeItemService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库条目")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:create')")
    public CommonResult<Long> createKnowledgeItem(@Valid @RequestBody AgentKnowledgeItemSaveReqVO createReqVO) {
        return success(knowledgeItemService.createKnowledgeItem(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库条目")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:update')")
    public CommonResult<Boolean> updateKnowledgeItem(@Valid @RequestBody AgentKnowledgeItemSaveReqVO updateReqVO) {
        knowledgeItemService.updateKnowledgeItem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库条目")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:delete')")
    public CommonResult<Boolean> deleteKnowledgeItem(@RequestParam("id") Long id) {
        knowledgeItemService.deleteKnowledgeItem(id);
        return success(true);
    }

    @PostMapping("/rebuild-index")
    @Operation(summary = "重建知识条目索引")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:update')")
    public CommonResult<Boolean> rebuildKnowledgeItemIndex(@RequestParam("id") Long id) {
        knowledgeItemService.rebuildKnowledgeItemIndex(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库条目")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:query')")
    public CommonResult<AgentKnowledgeItemRespVO> getKnowledgeItem(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(knowledgeItemService.getKnowledgeItem(id), AgentKnowledgeItemRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得知识库分页")
    @PreAuthorize("@ss.hasPermission('agent:knowledge:query')")
    public CommonResult<PageResult<AgentKnowledgeItemRespVO>> getKnowledgeItemPage(
            @Valid AgentKnowledgeItemPageReqVO pageReqVO) {
        PageResult<AgentKnowledgeItemDO> pageResult = knowledgeItemService.getKnowledgeItemPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AgentKnowledgeItemRespVO.class));
    }

}
