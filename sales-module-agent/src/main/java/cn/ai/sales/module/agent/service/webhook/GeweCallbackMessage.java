package cn.ai.sales.module.agent.service.webhook;

import java.time.LocalDateTime;

public record GeweCallbackMessage(
        String eventId,
        String eventType,
        String geweAppId,
        String ownerWxid,
        String contactWxid,
        String geweMessageId,
        Integer messageType,
        String content,
        String contactDisplayName,
        String groupDisplayName,
        String groupMemberWxid,
        String groupMemberDisplayName,
        LocalDateTime messageTime,
        boolean selfSent
) {
}
