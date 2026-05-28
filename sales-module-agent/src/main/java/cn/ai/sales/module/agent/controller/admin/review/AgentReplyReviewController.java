package cn.ai.sales.module.agent.controller.admin.review;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewApproveReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewPageReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewRejectReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewRespVO;
import cn.ai.sales.module.agent.service.review.AgentReplyReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 销冠回复审核")
@RestController
@RequestMapping("/agent/review")
@Validated
public class AgentReplyReviewController {

    @Resource
    private AgentReplyReviewService reviewService;

    @GetMapping("/page")
    @Operation(summary = "获得回复审核分页")
    @PreAuthorize("@ss.hasPermission('agent:review:query')")
    public CommonResult<PageResult<AgentReplyReviewRespVO>> getReviewPage(
            @Valid AgentReplyReviewPageReqVO pageReqVO) {
        return success(reviewService.getReviewPage(pageReqVO));
    }

    @PostMapping("/approve")
    @Operation(summary = "审核通过或修改后发送")
    @PreAuthorize("@ss.hasPermission('agent:review:update')")
    public CommonResult<Long> approve(@Valid @RequestBody AgentReplyReviewApproveReqVO approveReqVO) {
        return success(reviewService.approve(approveReqVO));
    }

    @PostMapping("/reject")
    @Operation(summary = "驳回回复建议")
    @PreAuthorize("@ss.hasPermission('agent:review:update')")
    public CommonResult<Boolean> reject(@Valid @RequestBody AgentReplyReviewRejectReqVO rejectReqVO) {
        reviewService.reject(rejectReqVO);
        return success(true);
    }

}
