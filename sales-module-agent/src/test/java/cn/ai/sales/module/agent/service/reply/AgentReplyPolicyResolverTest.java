package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentReplyPolicyResolverTest {

    private final AgentReplyPolicyResolver resolver = new AgentReplyPolicyResolver();

    @Test
    void resolveUsesAccountPolicyWhenContactDoesNotOverride() {
        AgentWechatAccountDO account = accountPolicy();
        AgentWechatContactDO contact = new AgentWechatContactDO();

        AgentReplyPolicy policy = resolver.resolve(account, contact);

        assertThat(policy.replyMode()).isEqualTo(AgentConstants.REPLY_MODE_AUTO_REPLY);
        assertThat(policy.quietSeconds()).isEqualTo(45);
        assertThat(policy.businessHours()).containsEntry("start", "09:00");
        assertThat(policy.source()).isEqualTo("ACCOUNT");
    }

    @Test
    void resolveUsesOnlyNonNullContactOverrides() {
        AgentWechatAccountDO account = accountPolicy();
        AgentWechatContactDO contact = new AgentWechatContactDO();
        contact.setReplyMode(AgentConstants.REPLY_MODE_MANUAL_CONFIRM);
        contact.setQuietSeconds(90);

        AgentReplyPolicy policy = resolver.resolve(account, contact);

        assertThat(policy.replyMode()).isEqualTo(AgentConstants.REPLY_MODE_MANUAL_CONFIRM);
        assertThat(policy.quietSeconds()).isEqualTo(90);
        assertThat(policy.businessHours()).containsEntry("end", "18:00");
        assertThat(policy.source()).isEqualTo("CONTACT");
    }

    @Test
    void resolveNormalizesDeprecatedManualOnlyToManualConfirm() {
        AgentWechatAccountDO account = accountPolicy();
        account.setReplyMode(AgentConstants.REPLY_MODE_MANUAL_ONLY);

        AgentReplyPolicy policy = resolver.resolve(account, null);

        assertThat(policy.replyMode()).isEqualTo(AgentConstants.REPLY_MODE_MANUAL_CONFIRM);
    }

    @Test
    void resolveUsesConversationPolicyDefaultsWhenAccountDoesNotConfigureTiming() {
        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setReplyMode(AgentConstants.REPLY_MODE_AUTO_REPLY);

        AgentReplyPolicy policy = resolver.resolve(account, null);

        assertThat(policy.quietSeconds()).isEqualTo(AgentConstants.DEFAULT_QUIET_SECONDS);
        assertThat(policy.businessHours())
                .containsEntry("start", "08:00")
                .containsEntry("end", "22:00");
    }

    private AgentWechatAccountDO accountPolicy() {
        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setReplyMode(AgentConstants.REPLY_MODE_AUTO_REPLY);
        account.setQuietSeconds(45);
        account.setBusinessHours(Map.of("start", "09:00", "end", "18:00"));
        return account;
    }

}
