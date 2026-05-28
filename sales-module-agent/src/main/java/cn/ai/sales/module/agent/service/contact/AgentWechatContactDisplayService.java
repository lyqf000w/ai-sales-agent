package cn.ai.sales.module.agent.service.contact;

import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.service.conversation.AgentWechatDisplayFormatter;
import cn.ai.sales.module.agent.service.gewe.GeweContactInfo;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AgentWechatContactDisplayService {

    @Resource
    private AgentWechatAccountMapper accountMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private GeweMessageClient geweMessageClient;

    public String resolveDisplayName(AgentWechatContactDO contact) {
        if (contact == null) {
            return "";
        }
        return AgentWechatDisplayFormatter.resolveContactDisplayName(contact.getId(), contact.getExternalUserId(),
                contact.getRemark(), contact.getNickname(), contact.getWechatId());
    }

    public String refreshAndResolveDisplayName(AgentWechatContactDO contact) {
        refreshDisplayNameIfNeeded(contact);
        return resolveDisplayName(contact);
    }

    public void refreshDisplayNameIfNeeded(AgentWechatContactDO contact) {
        if (contact == null || !needsDisplayNameRefresh(contact)) {
            return;
        }
        AgentWechatAccountDO account = accountMapper.selectById(contact.getWechatAccountId());
        GeweContactInfo info = geweMessageClient.getContactInfo(account, contact.getExternalUserId());
        if (info == null) {
            return;
        }
        String nickname = AgentWechatDisplayFormatter.firstUsableName(contact.getExternalUserId(),
                info.remark(), info.nickname());
        if (StrUtil.isBlank(nickname) && StrUtil.isBlank(info.avatar())) {
            return;
        }
        AgentWechatContactDO update = new AgentWechatContactDO();
        update.setId(contact.getId());
        if (StrUtil.isNotBlank(nickname)) {
            update.setNickname(nickname);
            contact.setNickname(nickname);
        }
        if (AgentWechatDisplayFormatter.isUsableName(info.remark(), contact.getExternalUserId())) {
            update.setRemark(info.remark());
            contact.setRemark(info.remark());
        }
        if (StrUtil.isNotBlank(info.avatar())) {
            update.setAvatar(info.avatar());
            contact.setAvatar(info.avatar());
        }
        contactMapper.updateById(update);
    }

    private boolean needsDisplayNameRefresh(AgentWechatContactDO contact) {
        String name = StrUtil.firstNonBlank(contact.getRemark(), contact.getNickname(), contact.getWechatId());
        return !AgentWechatDisplayFormatter.isUsableName(name, contact.getExternalUserId());
    }

}
