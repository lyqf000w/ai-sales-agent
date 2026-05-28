package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentReplyPolicyResolver {

    public static final Map<String, Object> DEFAULT_BUSINESS_HOURS = Map.of("start",
            AgentConstants.DEFAULT_BUSINESS_HOURS_START, "end", AgentConstants.DEFAULT_BUSINESS_HOURS_END);

    public AgentReplyPolicy resolve(AgentWechatAccountDO account, AgentWechatContactDO contact) {
        String replyMode = account == null || account.getReplyMode() == null
                ? AgentConstants.REPLY_MODE_MANUAL_CONFIRM : account.getReplyMode();
        Integer quietSeconds = account == null
                ? AgentConstants.DEFAULT_QUIET_SECONDS : resolveQuietSeconds(account.getQuietSeconds(), account.getQuietMinutes());
        Map<String, Object> businessHours = account == null || account.getBusinessHours() == null
                ? DEFAULT_BUSINESS_HOURS : account.getBusinessHours();
        String source = AgentReplyPolicy.SOURCE_ACCOUNT;

        if (contact != null) {
            boolean hasContactOverride = false;
            if (contact.getReplyMode() != null) {
                replyMode = contact.getReplyMode();
                hasContactOverride = true;
            }
            if (contact.getQuietSeconds() != null || contact.getQuietMinutes() != null) {
                quietSeconds = resolveQuietSeconds(contact.getQuietSeconds(), contact.getQuietMinutes());
                hasContactOverride = true;
            }
            if (contact.getBusinessHours() != null) {
                businessHours = contact.getBusinessHours();
                hasContactOverride = true;
            }
            if (hasContactOverride) {
                source = AgentReplyPolicy.SOURCE_CONTACT;
            }
        }

        return new AgentReplyPolicy(normalizeReplyMode(replyMode), quietSeconds, businessHours, source);
    }

    private Integer resolveQuietSeconds(Integer quietSeconds, Integer legacyQuietMinutes) {
        if (quietSeconds != null) {
            return quietSeconds;
        }
        if (legacyQuietMinutes != null) {
            return legacyQuietMinutes * 60;
        }
        return AgentConstants.DEFAULT_QUIET_SECONDS;
    }

    private String normalizeReplyMode(String replyMode) {
        if (AgentConstants.REPLY_MODE_MANUAL_ONLY.equals(replyMode)) {
            return AgentConstants.REPLY_MODE_MANUAL_CONFIRM;
        }
        if (AgentConstants.REPLY_MODE_AUTO_REPLY.equals(replyMode)
                || AgentConstants.REPLY_MODE_RECORD_ONLY.equals(replyMode)
                || AgentConstants.REPLY_MODE_MANUAL_CONFIRM.equals(replyMode)) {
            return replyMode;
        }
        return AgentConstants.REPLY_MODE_MANUAL_CONFIRM;
    }

}
