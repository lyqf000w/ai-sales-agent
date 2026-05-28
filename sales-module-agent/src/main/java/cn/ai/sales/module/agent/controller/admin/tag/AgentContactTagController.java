package cn.ai.sales.module.agent.controller.admin.tag;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.tag.vo.AgentContactTagPageReqVO;
import cn.ai.sales.module.agent.controller.admin.tag.vo.AgentContactTagRespVO;
import cn.ai.sales.module.agent.controller.admin.tag.vo.AgentContactTagSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentContactTagDO;
import cn.ai.sales.module.agent.service.tag.AgentContactTagService;
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

@Tag(name = "管理后台 - AI 销冠客户标签")
@RestController
@RequestMapping("/agent/tag")
@Validated
public class AgentContactTagController {

    @Resource
    private AgentContactTagService tagService;

    @PostMapping("/create")
    @Operation(summary = "创建客户标签")
    @PreAuthorize("@ss.hasPermission('agent:tag:create')")
    public CommonResult<Long> createTag(@Valid @RequestBody AgentContactTagSaveReqVO createReqVO) {
        return success(tagService.createTag(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户标签")
    @PreAuthorize("@ss.hasPermission('agent:tag:update')")
    public CommonResult<Boolean> updateTag(@Valid @RequestBody AgentContactTagSaveReqVO updateReqVO) {
        tagService.updateTag(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户标签")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:tag:delete')")
    public CommonResult<Boolean> deleteTag(@RequestParam("id") Long id) {
        tagService.deleteTag(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户标签")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:tag:query')")
    public CommonResult<AgentContactTagRespVO> getTag(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(tagService.getTag(id), AgentContactTagRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户标签分页")
    @PreAuthorize("@ss.hasPermission('agent:tag:query')")
    public CommonResult<PageResult<AgentContactTagRespVO>> getTagPage(@Valid AgentContactTagPageReqVO pageReqVO) {
        PageResult<AgentContactTagDO> pageResult = tagService.getTagPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AgentContactTagRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得可用客户标签列表")
    @PreAuthorize("@ss.hasPermission('agent:tag:query')")
    public CommonResult<List<AgentContactTagRespVO>> getSimpleTagList() {
        return success(BeanUtils.toBean(tagService.getEnabledTags(), AgentContactTagRespVO.class));
    }

}
