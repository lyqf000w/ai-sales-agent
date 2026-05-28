package cn.ai.sales.module.agent.controller.admin.contact;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactPageReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactRespVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactSyncRespVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateLevelReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateOwnerReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateReplyPolicyReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateSalesInsightReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateTagsReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.service.contact.AgentWechatContactService;
import cn.ai.sales.module.agent.service.tag.AgentContactTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.ai.sales.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 销冠微信好友")
@RestController
@RequestMapping("/agent/contact")
@Validated
public class AgentWechatContactController {

    @Resource
    private AgentWechatContactService wechatContactService;
    @Resource
    private AgentContactTagService tagService;

    @GetMapping("/page")
    @Operation(summary = "获得微信好友分页")
    @PreAuthorize("@ss.hasPermission('agent:contact:query')")
    public CommonResult<PageResult<AgentWechatContactRespVO>> getWechatContactPage(
            @Valid AgentWechatContactPageReqVO pageReqVO) {
        PageResult<AgentWechatContactDO> pageResult = wechatContactService.getWechatContactPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AgentWechatContactRespVO.class));
    }

    @PostMapping("/sync")
    @Operation(summary = "Sync WeChat contacts from GeWe")
    @PreAuthorize("@ss.hasPermission('agent:contact:update') || @ss.hasPermission('agent:conversation:send')")
    public CommonResult<AgentWechatContactSyncRespVO> syncWechatContacts(
            @RequestParam(value = "wechatAccountId", required = false) Long wechatAccountId) {
        return success(wechatContactService.syncWechatContacts(wechatAccountId));
    }

    @PutMapping("/update-level")
    @Operation(summary = "更新微信好友客户等级")
    @PreAuthorize("@ss.hasPermission('agent:contact:update')")
    public CommonResult<Boolean> updateWechatContactLevel(
            @Valid @RequestBody AgentWechatContactUpdateLevelReqVO updateReqVO) {
        wechatContactService.updateWechatContactLevel(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-owner")
    @Operation(summary = "更新微信好友负责人")
    @PreAuthorize("@ss.hasPermission('agent:contact:update')")
    public CommonResult<Boolean> updateWechatContactOwner(
            @Valid @RequestBody AgentWechatContactUpdateOwnerReqVO updateReqVO) {
        wechatContactService.updateWechatContactOwner(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-reply-policy")
    @Operation(summary = "更新微信好友回复策略覆盖")
    @PreAuthorize("@ss.hasPermission('agent:contact:update')")
    public CommonResult<Boolean> updateWechatContactReplyPolicy(
            @Valid @RequestBody AgentWechatContactUpdateReplyPolicyReqVO updateReqVO) {
        wechatContactService.updateWechatContactReplyPolicy(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-sales-insight")
    @Operation(summary = "更新微信好友销售洞察")
    @PreAuthorize("@ss.hasPermission('agent:contact:update')")
    public CommonResult<Boolean> updateWechatContactSalesInsight(
            @Valid @RequestBody AgentWechatContactUpdateSalesInsightReqVO updateReqVO) {
        wechatContactService.updateWechatContactSalesInsight(updateReqVO);
        return success(true);
    }

    @GetMapping("/tags")
    @Operation(summary = "获得微信好友标签")
    @PreAuthorize("@ss.hasPermission('agent:contact:query')")
    public CommonResult<List<Long>> getWechatContactTags(@RequestParam("contactId") Long contactId) {
        return success(tagService.getContactTagIds(contactId));
    }

    @PutMapping("/tags")
    @Operation(summary = "更新微信好友标签")
    @PreAuthorize("@ss.hasPermission('agent:contact:update')")
    public CommonResult<Boolean> updateWechatContactTags(
            @Valid @RequestBody AgentWechatContactUpdateTagsReqVO updateReqVO) {
        tagService.updateContactTags(updateReqVO);
        return success(true);
    }

}
