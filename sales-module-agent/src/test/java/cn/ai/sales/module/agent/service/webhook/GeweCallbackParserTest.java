package cn.ai.sales.module.agent.service.webhook;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static java.util.Map.entry;

class GeweCallbackParserTest {

    private final GeweCallbackParser parser = new GeweCallbackParser();

    @Test
    void parseV2TextMessage() {
        Map<String, Object> payload = Map.of(
                "typeName", "AddMsg",
                "appid", "wx-app-001",
                "wxid", "wxid_owner",
                "newMsgId", 3768973957878705021L,
                "msgType", "TEXT",
                "fromUser", "wxid_customer",
                "toUser", "wxid_owner",
                "content", "hello, I want product info",
                "createTime", 1779163000L,
                "isSelf", false
        );

        GeweCallbackMessage message = parser.parse(payload);

        assertThat(message.eventId()).isEqualTo("wx-app-001:AddMsg:wxid_customer:3768973957878705021");
        assertThat(message.eventType()).isEqualTo("AddMsg");
        assertThat(message.geweAppId()).isEqualTo("wx-app-001");
        assertThat(message.ownerWxid()).isEqualTo("wxid_owner");
        assertThat(message.contactWxid()).isEqualTo("wxid_customer");
        assertThat(message.geweMessageId()).isEqualTo("3768973957878705021");
        assertThat(message.messageType()).isEqualTo(1);
        assertThat(message.content()).isEqualTo("hello, I want product info");
    }

    @Test
    void parseV2SelfSentMessageUsesToUserAsContact() {
        Map<String, Object> payload = Map.of(
                "typeName", "AddMsg",
                "appid", "wx-app-001",
                "wxid", "wxid_owner",
                "newMsgId", 10001L,
                "msgType", 1,
                "fromUser", "wxid_owner",
                "toUser", "wxid_customer",
                "content", "I sent you the files",
                "createTime", 1779163001L,
                "isSelf", true
        );

        GeweCallbackMessage message = parser.parse(payload);

        assertThat(message.selfSent()).isTrue();
        assertThat(message.contactWxid()).isEqualTo("wxid_customer");
    }

    @Test
    void parseWrappedWechatScalarFields() {
        Map<String, Object> payload = Map.of(
                "typeName", "AddMsg",
                "appid", Map.of("string", "wx-app-001"),
                "wxid", Map.of("string", "wxid_owner"),
                "newMsgId", Map.of("long", 10002L),
                "msgType", Map.of("int", 1),
                "fromUser", Map.of("string", "20666784639@chatroom"),
                "toUser", Map.of("string", "wxid_owner"),
                "content", Map.of("string", "wxid_customer:\nhello"),
                "createTime", Map.of("long", 1779163002L),
                "isSelf", Map.of("bool", false)
        );

        GeweCallbackMessage message = parser.parse(payload);

        assertThat(message.geweAppId()).isEqualTo("wx-app-001");
        assertThat(message.ownerWxid()).isEqualTo("wxid_owner");
        assertThat(message.contactWxid()).isEqualTo("20666784639@chatroom");
        assertThat(message.geweMessageId()).isEqualTo("10002");
        assertThat(message.messageType()).isEqualTo(1);
        assertThat(message.content()).isEqualTo("wxid_customer:\nhello");
        assertThat(message.selfSent()).isFalse();
    }

    @Test
    void parseGroupMessageUsesChatroomIdInsteadOfMemberWxid() {
        Map<String, Object> payload = Map.ofEntries(
                entry("typeName", "AddMsg"),
                entry("appid", "wx-app-001"),
                entry("wxid", "wxid_owner"),
                entry("newMsgId", 10004L),
                entry("msgType", 1),
                entry("fromUser", "wxid_group_member"),
                entry("toUser", "wxid_owner"),
                entry("chatroomId", "20666784639@chatroom"),
                entry("chatroomName", "Green Wind"),
                entry("content", "wxid_group_member:\nhello from group"),
                entry("createTime", 1779163004L),
                entry("isSelf", false)
        );

        GeweCallbackMessage message = parser.parse(payload);

        assertThat(message.contactWxid()).isEqualTo("20666784639@chatroom");
        assertThat(message.groupDisplayName()).isEqualTo("Green Wind");
        assertThat(message.groupMemberWxid()).isEqualTo("wxid_group_member");
        assertThat(message.content()).isEqualTo("wxid_group_member:\nhello from group");
    }

    @Test
    void parseGroupMessageUsesFromGroupAsChatroomId() {
        Map<String, Object> payload = Map.ofEntries(
                entry("eventCode", "group_msg_event"),
                entry("appid", "wx-app-001"),
                entry("wxid", "wxid_owner"),
                entry("newMsgId", 10008L),
                entry("msgType", 1),
                entry("fromUser", "wxid_group_member"),
                entry("toUser", "wxid_owner"),
                entry("fromGroup", "20666784639@chatroom"),
                entry("content", "hello from group"),
                entry("createTime", 1779163008L),
                entry("isSelf", false)
        );

        GeweCallbackMessage message = parser.parse(payload);

        assertThat(message.eventType()).isEqualTo("group_msg_event");
        assertThat(message.contactWxid()).isEqualTo("20666784639@chatroom");
        assertThat(message.groupMemberWxid()).isEqualTo("wxid_group_member");
    }

    @Test
    void parseSelfSentGroupMessageUsesChatroomIdInsteadOfOwnerWxid() {
        Map<String, Object> payload = Map.ofEntries(
                entry("typeName", "AddMsg"),
                entry("appid", "wx-app-001"),
                entry("wxid", "wxid_owner"),
                entry("newMsgId", 10005L),
                entry("msgType", 1),
                entry("fromUser", "wxid_owner"),
                entry("toUser", "wxid_group_member"),
                entry("Data", Map.of("roomWxid", Map.of("string", "20666784639@chatroom"))),
                entry("content", "reply in group"),
                entry("createTime", 1779163005L),
                entry("isSelf", true)
        );

        GeweCallbackMessage message = parser.parse(payload);

        assertThat(message.selfSent()).isTrue();
        assertThat(message.contactWxid()).isEqualTo("20666784639@chatroom");
    }

    @Test
    void parseJsonWrappedWechatScalarFields() {
        Map<String, Object> payload = Map.of(
                "typeName", "AddMsg",
                "appid", "{\"string\":\"wx-app-001\"}",
                "wxid", "{\"string\":\"wxid_owner\"}",
                "newMsgId", "{\"long\":10003}",
                "msgType", "{\"int\":1}",
                "fromUser", "{\"string\":\"wxid_customer\"}",
                "toUser", "{\"string\":\"wxid_owner\"}",
                "content", "{\"string\":\"hello\\\\nfrom wechat\"}",
                "createTime", "{\"long\":1779163003}",
                "isSelf", "{\"bool\":false}"
        );

        GeweCallbackMessage message = parser.parse(payload);

        assertThat(message.geweAppId()).isEqualTo("wx-app-001");
        assertThat(message.ownerWxid()).isEqualTo("wxid_owner");
        assertThat(message.contactWxid()).isEqualTo("wxid_customer");
        assertThat(message.geweMessageId()).isEqualTo("10003");
        assertThat(message.messageType()).isEqualTo(1);
        assertThat(message.content()).isEqualTo("hello\nfrom wechat");
        assertThat(message.selfSent()).isFalse();
    }

    @Test
    void parseImageMessageFromXmlContentWhenTypeMissing() {
        Map<String, Object> payload = Map.of(
                "typeName", "AddMsg",
                "appid", "wx-app-001",
                "wxid", "wxid_owner",
                "newMsgId", 10006L,
                "fromUser", "wxid_customer",
                "toUser", "wxid_owner",
                "Data", Map.of("MsgContent",
                        "<msg><img cdnthumburl=\"https://cdn.example.com/thumb.jpg\" cdnbigimgurl=\"https://cdn.example.com/big.jpg\" /></msg>"),
                "createTime", 1779163006L,
                "isSelf", false
        );

        GeweCallbackMessage message = parser.parse(payload);

        assertThat(message.messageType()).isEqualTo(3);
        assertThat(message.content()).contains("cdnbigimgurl");
    }

    @Test
    void parseEmojiMessageFromXmlContentWhenTypeMissing() {
        Map<String, Object> payload = Map.of(
                "typeName", "AddMsg",
                "appid", "wx-app-001",
                "wxid", "wxid_owner",
                "newMsgId", 10007L,
                "fromUser", "wxid_customer",
                "toUser", "wxid_owner",
                "Data", Map.of("Xml", "<msg><emoji cdnurl=\"https://cdn.example.com/sticker.gif\" /></msg>"),
                "createTime", 1779163007L,
                "isSelf", false
        );

        GeweCallbackMessage message = parser.parse(payload);

        assertThat(message.messageType()).isEqualTo(47);
        assertThat(message.content()).contains("sticker.gif");
    }

}
