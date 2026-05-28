package cn.ai.sales.module.agent.service.contact;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactPageReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactSyncRespVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateLevelReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateOwnerReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateReplyPolicyReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateSalesInsightReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.gewe.GeweContactInfo;
import cn.ai.sales.module.agent.service.gewe.GeweContactListResult;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.ai.sales.module.agent.service.reply.AgentReplyPolicyActivationService;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WECHAT_ACCOUNT_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WECHAT_CONTACT_NOT_EXISTS;

@Service
@Validated
public class AgentWechatContactServiceImpl implements AgentWechatContactService {

    @Resource
    private AgentWechatContactMapper wechatContactMapper;
    @Resource
    private AgentWechatAccountMapper accountMapper;
    @Resource
    private GeweMessageClient geweMessageClient;
    @Resource
    private AgentReplyPolicyActivationService replyPolicyActivationService;

    @Override
    public AgentWechatContactDO getWechatContact(Long id) {
        return wechatContactMapper.selectById(id);
    }

    @Override
    public PageResult<AgentWechatContactDO> getWechatContactPage(AgentWechatContactPageReqVO pageReqVO) {
        return wechatContactMapper.selectPage(pageReqVO);
    }

    @Override
    public AgentWechatContactSyncRespVO syncWechatContacts(Long wechatAccountId) {
        List<AgentWechatAccountDO> accounts;
        if (wechatAccountId != null) {
            AgentWechatAccountDO account = accountMapper.selectById(wechatAccountId);
            if (account == null) {
                throw exception(WECHAT_ACCOUNT_NOT_EXISTS);
            }
            accounts = List.of(account);
        } else {
            accounts = accountMapper.selectSyncableList();
        }
        AgentWechatContactSyncRespVO total = new AgentWechatContactSyncRespVO();
        for (AgentWechatAccountDO account : accounts) {
            total.add(syncAccountContacts(account));
        }
        return total;
    }

    @Override
    public void updateWechatContactLevel(AgentWechatContactUpdateLevelReqVO updateReqVO) {
        validateWechatContactExists(updateReqVO.getId());
        AgentWechatContactDO updateObj = new AgentWechatContactDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setCustomerLevel(updateReqVO.getCustomerLevel());
        wechatContactMapper.updateById(updateObj);
    }

    @Override
    public void updateWechatContactOwner(AgentWechatContactUpdateOwnerReqVO updateReqVO) {
        validateWechatContactExists(updateReqVO.getId());
        AgentWechatContactDO updateObj = new AgentWechatContactDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setOwnerUserId(updateReqVO.getOwnerUserId());
        wechatContactMapper.updateById(updateObj);
    }

    @Override
    public void updateWechatContactReplyPolicy(AgentWechatContactUpdateReplyPolicyReqVO updateReqVO) {
        AgentWechatContactDO contact = validateWechatContactExists(updateReqVO.getId());
        String replyMode = AgentConstants.REPLY_MODE_MANUAL_ONLY.equals(updateReqVO.getReplyMode())
                ? AgentConstants.REPLY_MODE_MANUAL_CONFIRM : updateReqVO.getReplyMode();
        AgentWechatContactDO updateObj = new AgentWechatContactDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setReplyMode(replyMode);
        updateObj.setQuietSeconds(updateReqVO.getQuietSeconds());
        updateObj.setBusinessHours(updateReqVO.getBusinessHours());
        wechatContactMapper.updateReplyPolicyById(updateObj);
        if (AgentConstants.REPLY_MODE_AUTO_REPLY.equals(replyMode)) {
            replyPolicyActivationService.activateContactAutoReply(contact.getWechatAccountId(), contact.getId(), null);
        }
    }

    @Override
    public void updateWechatContactSalesInsight(AgentWechatContactUpdateSalesInsightReqVO updateReqVO) {
        validateWechatContactExists(updateReqVO.getId());
        AgentWechatContactDO updateObj = new AgentWechatContactDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setPurchaseIntention(defaultIfBlank(updateReqVO.getPurchaseIntention(),
                AgentConstants.PURCHASE_INTENTION_MEDIUM));
        updateObj.setSalesStage(defaultIfBlank(updateReqVO.getSalesStage(), AgentConstants.SALES_STAGE_NEW_LEAD));
        updateObj.setCustomerSentiment(defaultIfBlank(updateReqVO.getCustomerSentiment(),
                AgentConstants.CUSTOMER_SENTIMENT_NEUTRAL));
        updateObj.setFollowUpPriority(defaultIfBlank(updateReqVO.getFollowUpPriority(),
                AgentConstants.FOLLOW_UP_PRIORITY_NORMAL));
        wechatContactMapper.updateById(updateObj);
    }

    private AgentWechatContactDO validateWechatContactExists(Long id) {
        AgentWechatContactDO contact = wechatContactMapper.selectById(id);
        if (contact == null) {
            throw exception(WECHAT_CONTACT_NOT_EXISTS);
        }
        return contact;
    }

    private AgentWechatContactSyncRespVO syncAccountContacts(AgentWechatAccountDO account) {
        AgentWechatContactSyncRespVO respVO = new AgentWechatContactSyncRespVO();
        respVO.setAccountCount(1);
        GeweContactListResult contactList = geweMessageClient.getContactList(account);
        List<String> contactWxids = contactList.syncableContactIds().stream()
                .filter(this::isSyncableContactId)
                .toList();
        respVO.setFetchedCount(contactWxids.size());
        Map<String, GeweContactInfo> infoMap = geweMessageClient.getContactInfoMap(account, contactWxids);
        Map<String, AgentWechatContactDO> existedMap = wechatContactMapper.selectListByWechatAccountId(account.getId())
                .stream()
                .filter(contact -> StrUtil.isNotBlank(contact.getExternalUserId()))
                .collect(java.util.stream.Collectors.toMap(AgentWechatContactDO::getExternalUserId,
                        contact -> contact, (left, right) -> left));
        int created = 0;
        int updated = 0;
        int skipped = 0;
        for (String wxid : contactWxids) {
            if (StrUtil.equals(wxid, account.getWechatId())) {
                skipped++;
                continue;
            }
            GeweContactInfo info = infoMap.get(wxid);
            AgentWechatContactDO existed = existedMap.get(wxid);
            if (existed == null) {
                wechatContactMapper.insert(buildContact(account, wxid, info));
                created++;
            } else if (updateContactDisplayIfNeeded(existed, info)) {
                updated++;
            }
        }
        respVO.setCreatedCount(created);
        respVO.setUpdatedCount(updated);
        respVO.setSkippedCount(skipped);
        return respVO;
    }

    private AgentWechatContactDO buildContact(AgentWechatAccountDO account, String wxid, GeweContactInfo info) {
        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setWechatAccountId(account.getId());
        contact.setExternalUserId(wxid);
        contact.setWechatId(wxid);
        applyDisplayInfo(contact, info);
        contact.setOwnerUserId(account.getOwnerUserId());
        contact.setCustomerLevel(AgentConstants.CUSTOMER_LEVEL_NORMAL);
        contact.setRiskLevel(AgentConstants.RISK_LEVEL_GREEN);
        contact.setLastConversationStatus(AgentConstants.CONVERSATION_STATUS_OPEN);
        contact.setPurchaseIntention(AgentConstants.PURCHASE_INTENTION_MEDIUM);
        contact.setSalesStage(AgentConstants.SALES_STAGE_NEW_LEAD);
        contact.setCustomerSentiment(AgentConstants.CUSTOMER_SENTIMENT_NEUTRAL);
        contact.setFollowUpPriority(AgentConstants.FOLLOW_UP_PRIORITY_NORMAL);
        return contact;
    }

    private boolean updateContactDisplayIfNeeded(AgentWechatContactDO contact, GeweContactInfo info) {
        if (info == null) {
            return false;
        }
        AgentWechatContactDO update = new AgentWechatContactDO();
        update.setId(contact.getId());
        boolean changed = false;
        if (StrUtil.isBlank(contact.getWechatId())) {
            update.setWechatId(contact.getExternalUserId());
            changed = true;
        }
        if (StrUtil.isBlank(contact.getNickname()) && StrUtil.isNotBlank(info.nickname())) {
            update.setNickname(info.nickname());
            changed = true;
        }
        if (StrUtil.isBlank(contact.getRemark()) && StrUtil.isNotBlank(info.remark())) {
            update.setRemark(info.remark());
            changed = true;
        }
        if (StrUtil.isBlank(contact.getAvatar()) && StrUtil.isNotBlank(info.avatar())) {
            update.setAvatar(info.avatar());
            changed = true;
        }
        if (changed) {
            wechatContactMapper.updateById(update);
        }
        return changed;
    }

    private void applyDisplayInfo(AgentWechatContactDO contact, GeweContactInfo info) {
        if (info == null) {
            return;
        }
        contact.setNickname(info.nickname());
        contact.setRemark(info.remark());
        contact.setAvatar(info.avatar());
    }

    private boolean isSyncableContactId(String wxid) {
        if (StrUtil.isBlank(wxid)) {
            return false;
        }
        String text = wxid.trim();
        return !StrUtil.equalsAnyIgnoreCase(text, "weixin", "fmessage", "floatbottle", "medianote")
                && !StrUtil.startWithIgnoreCase(text, "gh_");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

}
