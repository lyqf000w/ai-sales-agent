package cn.ai.sales.module.agent.controller.admin.replypolicy;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.module.agent.controller.admin.replypolicy.vo.AgentReplyPolicyRespVO;
import cn.ai.sales.module.agent.controller.admin.replypolicy.vo.AgentReplyPolicySaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.reply.AgentReplyPolicy;
import cn.ai.sales.module.agent.service.reply.AgentReplyPolicyActivationService;
import cn.ai.sales.module.agent.service.reply.AgentReplyPolicyResolver;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.framework.common.pojo.CommonResult.success;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WECHAT_ACCOUNT_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WECHAT_CONTACT_NOT_EXISTS;

@Tag(name = "管理后台 - 个人销售助手回复策略")
@RestController
@RequestMapping("/agent/reply-policy")
@Validated
public class AgentReplyPolicyController {

    @Resource
    private AgentWechatAccountMapper accountMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private AgentConversationMapper conversationMapper;
    @Resource
    private AgentReplyPolicyResolver replyPolicyResolver;
    @Resource
    private AgentReplyPolicyActivationService replyPolicyActivationService;

    @PostMapping("/save")
    @Operation(summary = "保存账号默认策略或客户会话覆盖策略")
    @PreAuthorize("@ss.hasPermission('agent:contact:update') || @ss.hasPermission('agent:conversation:send')")
    public CommonResult<AgentReplyPolicyRespVO> save(@Valid @RequestBody AgentReplyPolicySaveReqVO reqVO) {
        AgentWechatContactDO contact = resolveContact(reqVO.getContactId(), reqVO.getConversationId());
        if (contact != null) {
            AgentWechatContactDO update = new AgentWechatContactDO();
            update.setId(contact.getId());
            update.setReplyMode(normalizeContactReplyMode(reqVO.getReplyMode()));
            update.setQuietSeconds(reqVO.getQuietSeconds());
            update.setBusinessHours(reqVO.getBusinessHours());
            contactMapper.updateReplyPolicyById(update);
            contact = contactMapper.selectById(contact.getId());
            AgentWechatAccountDO account = resolveAccount(contact.getWechatAccountId());
            AgentReplyPolicyRespVO respVO = toResp(account, contact, reqVO.getConversationId());
            activateAutoReplyIfNeeded(respVO, account.getId(), contact.getId(), reqVO.getConversationId());
            return success(respVO);
        }

        Long accountId = reqVO.getWechatAccountId();
        if (accountId == null) {
            throw exception(WECHAT_CONTACT_NOT_EXISTS);
        }
        AgentWechatAccountDO account = resolveAccount(accountId);
        AgentWechatAccountDO update = new AgentWechatAccountDO();
        update.setId(accountId);
        update.setReplyMode(normalizeAccountReplyMode(reqVO.getReplyMode()));
        update.setQuietSeconds(resolveQuietSeconds(reqVO.getQuietSeconds()));
        update.setBusinessHours(resolveBusinessHours(reqVO.getBusinessHours()));
        accountMapper.updateById(update);
        AgentWechatAccountDO latestAccount = accountMapper.selectById(accountId);
        AgentReplyPolicyRespVO respVO = toResp(latestAccount, null, null);
        activateAutoReplyIfNeeded(respVO, latestAccount.getId(), null, null);
        return success(respVO);
    }

    @GetMapping("/resolve")
    @Operation(summary = "解析最终生效回复策略")
    @PreAuthorize("@ss.hasPermission('agent:contact:query') || @ss.hasPermission('agent:conversation:query')")
    public CommonResult<AgentReplyPolicyRespVO> resolve(
            @RequestParam(value = "wechatAccountId", required = false) Long wechatAccountId,
            @RequestParam(value = "contactId", required = false) Long contactId,
            @RequestParam(value = "conversationId", required = false) Long conversationId) {
        AgentWechatContactDO contact = resolveContact(contactId, conversationId);
        AgentWechatAccountDO account = contact != null ? resolveAccount(contact.getWechatAccountId())
                : wechatAccountId == null ? null : resolveAccount(wechatAccountId);
        return success(toResp(account, contact, conversationId));
    }

    private AgentReplyPolicyRespVO toResp(AgentWechatAccountDO account, AgentWechatContactDO contact,
                                          Long conversationId) {
        AgentReplyPolicy policy = replyPolicyResolver.resolve(account, contact);
        AgentReplyPolicyRespVO respVO = new AgentReplyPolicyRespVO();
        respVO.setWechatAccountId(contact != null ? contact.getWechatAccountId() : account == null ? null : account.getId());
        respVO.setContactId(contact == null ? null : contact.getId());
        respVO.setConversationId(conversationId);
        respVO.setReplyMode(policy.replyMode());
        respVO.setQuietSeconds(policy.quietSeconds());
        respVO.setBusinessHours(policy.businessHours());
        respVO.setSource(policy.source());
        return respVO;
    }

    private AgentWechatContactDO resolveContact(Long contactId, Long conversationId) {
        if (contactId != null) {
            AgentWechatContactDO contact = contactMapper.selectById(contactId);
            if (contact == null) {
                throw exception(WECHAT_CONTACT_NOT_EXISTS);
            }
            return contact;
        }
        if (conversationId == null) {
            return null;
        }
        AgentConversationDO conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || conversation.getContactId() == null) {
            throw exception(WECHAT_CONTACT_NOT_EXISTS);
        }
        AgentWechatContactDO contact = contactMapper.selectById(conversation.getContactId());
        if (contact == null) {
            throw exception(WECHAT_CONTACT_NOT_EXISTS);
        }
        return contact;
    }

    private AgentWechatAccountDO resolveAccount(Long accountId) {
        AgentWechatAccountDO account = accountMapper.selectById(accountId);
        if (account == null) {
            throw exception(WECHAT_ACCOUNT_NOT_EXISTS);
        }
        return account;
    }

    private String normalizeAccountReplyMode(String replyMode) {
        if (AgentConstants.REPLY_MODE_MANUAL_ONLY.equals(replyMode) || StrUtil.isBlank(replyMode)) {
            return AgentConstants.REPLY_MODE_MANUAL_CONFIRM;
        }
        return replyMode;
    }

    private String normalizeContactReplyMode(String replyMode) {
        if (StrUtil.isBlank(replyMode)) {
            return null;
        }
        if (AgentConstants.REPLY_MODE_MANUAL_ONLY.equals(replyMode)) {
            return AgentConstants.REPLY_MODE_MANUAL_CONFIRM;
        }
        return replyMode;
    }

    private Integer resolveQuietSeconds(Integer quietSeconds) {
        return quietSeconds == null || quietSeconds <= 0 ? AgentConstants.DEFAULT_QUIET_SECONDS : quietSeconds;
    }

    private Map<String, Object> resolveBusinessHours(Map<String, Object> businessHours) {
        return businessHours == null || businessHours.isEmpty()
                ? AgentReplyPolicyResolver.DEFAULT_BUSINESS_HOURS : businessHours;
    }

    private void activateAutoReplyIfNeeded(AgentReplyPolicyRespVO policy, Long wechatAccountId, Long contactId,
                                           Long conversationId) {
        if (!AgentConstants.REPLY_MODE_AUTO_REPLY.equals(policy.getReplyMode())) {
            return;
        }
        if (contactId != null) {
            replyPolicyActivationService.activateContactAutoReply(wechatAccountId, contactId, conversationId);
            return;
        }
        replyPolicyActivationService.activateAccountAutoReply(wechatAccountId);
    }

}
