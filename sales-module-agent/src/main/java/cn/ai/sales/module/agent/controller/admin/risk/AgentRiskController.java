package cn.ai.sales.module.agent.controller.admin.risk;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentMessageRespVO;
import cn.ai.sales.module.agent.controller.admin.risk.vo.AgentRiskConversationHandleReqVO;
import cn.ai.sales.module.agent.controller.admin.risk.vo.AgentRiskConversationRespVO;
import cn.ai.sales.module.agent.controller.admin.risk.vo.AgentRiskPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.service.risk.AgentRiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 销冠风险会话")
@RestController
@RequestMapping("/agent/risk")
@Validated
public class AgentRiskController {

    @Resource
    private AgentRiskService riskService;

    @GetMapping("/page")
    @Operation(summary = "获得风险会话分页")
    @PreAuthorize("@ss.hasPermission('agent:risk:query')")
    public CommonResult<PageResult<AgentRiskConversationRespVO>> getRiskPage(@Valid AgentRiskPageReqVO pageReqVO) {
        PageResult<AgentConversationDO> pageResult = riskService.getRiskPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AgentRiskConversationRespVO.class));
    }

    @GetMapping("/messages")
    @Operation(summary = "获得风险会话消息列表")
    @Parameter(name = "conversationId", description = "会话编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:risk:query')")
    public CommonResult<List<AgentMessageRespVO>> getRiskMessages(@RequestParam("conversationId") Long conversationId) {
        List<AgentMessageDO> messages = riskService.getRiskMessages(conversationId);
        return success(BeanUtils.toBean(messages, AgentMessageRespVO.class));
    }

    @PostMapping("/takeover")
    @Operation(summary = "人工接管风险会话")
    @PreAuthorize("@ss.hasPermission('agent:risk:update')")
    public CommonResult<Boolean> takeover(@Valid @RequestBody AgentRiskConversationHandleReqVO reqVO) {
        riskService.takeover(reqVO.getConversationId());
        return success(true);
    }

    @PostMapping("/close")
    @Operation(summary = "关闭风险会话")
    @PreAuthorize("@ss.hasPermission('agent:risk:update')")
    public CommonResult<Boolean> close(@Valid @RequestBody AgentRiskConversationHandleReqVO reqVO) {
        riskService.close(reqVO.getConversationId());
        return success(true);
    }

}
