package cn.ai.sales.module.agent.service.conversation;

import cn.ai.sales.module.agent.enums.AgentConstants;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentWechatDisplayFormatterTest {

    @Test
    void cleanScalarSupportsMapStyleAndJsonStyleWrappers() {
        assertThat(AgentWechatDisplayFormatter.cleanScalar("{string=wxid_customer}"))
                .isEqualTo("wxid_customer");
        assertThat(AgentWechatDisplayFormatter.cleanScalar("{\"string\":\"hello\\\\nwechat\"}"))
                .isEqualTo("hello\nwechat");
    }

    @Test
    void resolveContactDisplayNameSkipsRawWechatIdentifiers() {
        assertThat(AgentWechatDisplayFormatter.resolveContactDisplayName(7L, "wxid_customer",
                "{string=wxid_customer}", "wxid_customer", "张雨"))
                .isEqualTo("张雨");
        assertThat(AgentWechatDisplayFormatter.resolveContactDisplayName(8L, "20666784639@chatroom",
                "20666784639@chatroom", "weixin"))
                .isEqualTo("\u5ba2\u6237 #8");
    }

    @Test
    void formatMessageContentUsesWechatTitleForXmlPayload() {
        String content = "{\"string\":\"<msg><appmsg><title>product card</title><des /></appmsg></msg>\"}";

        String formatted = AgentWechatDisplayFormatter.formatMessageContent(
                AgentConstants.MESSAGE_TYPE_FILE_OR_LINK, content);

        assertThat(formatted).isEqualTo("product card");
    }

    @Test
    void formatMessageContentHidesRawXmlWhenNoReadableSummaryExists() {
        String formatted = AgentWechatDisplayFormatter.formatMessageContent(
                AgentConstants.MESSAGE_TYPE_FILE_OR_LINK, "<msg><appmsg /></msg>");

        assertThat(formatted).isEqualTo("\u005b\u94fe\u63a5/\u5c0f\u7a0b\u5e8f/\u6587\u4ef6\u005d");
    }

    @Test
    void replaceGroupMemberWxidPrefixUsesReadableName() {
        String formatted = AgentWechatDisplayFormatter.replaceGroupMemberWxidPrefix(
                "wxid_member:\nhello", "张雨");

        assertThat(formatted).isEqualTo("张雨:\nhello");
    }

    @Test
    void replaceGroupMemberWxidPrefixSupportsEscapedNewline() {
        String formatted = AgentWechatDisplayFormatter.replaceGroupMemberWxidPrefix(
                "wxid_member:\\nhello", "member name");

        assertThat(formatted).isEqualTo("member name:\nhello");
    }

    @Test
    void replaceGroupMemberWxidPrefixDoesNotExposeWxidWhenNameMissing() {
        String formatted = AgentWechatDisplayFormatter.replaceGroupMemberWxidPrefix(
                "wxid_member:\nhello", null);

        assertThat(formatted).isEqualTo("群成员:\nhello");
    }

    @Test
    void stripGroupSenderPrefixRemovesReadableSpeakerNameFromBubbleContent() {
        String content = AgentWechatDisplayFormatter.stripGroupSenderPrefix("Rain Zhang:\nhello", "Rain Zhang");

        assertThat(content).isEqualTo("hello");
    }

    @Test
    void isGroupMessageChecksChatroomPayloadBeforeTreatingReadablePrefixAsSpeaker() {
        assertThat(AgentWechatDisplayFormatter.isGroupMessage("Rain Zhang:\nhello",
                Map.of("chatroomId", "20666784639@chatroom"))).isTrue();
        assertThat(AgentWechatDisplayFormatter.isGroupMessage("Title:\nhello", Map.of())).isFalse();
    }

    @Test
    void extractMediaUrlFindsRenderableUrlInNestedPayload() {
        Map<String, Object> payload = Map.of("Data", Map.of("img", Map.of(
                "cdnUrl", "https://cdn.example.com/image.jpg",
                "cdnThumbUrl", "https://cdn.example.com/thumb.jpg",
                "fileName", "quote.jpg"
        )));

        assertThat(AgentWechatDisplayFormatter.extractMediaUrl(AgentConstants.MESSAGE_TYPE_IMAGE, "", payload))
                .isEqualTo("https://cdn.example.com/image.jpg");
        assertThat(AgentWechatDisplayFormatter.extractThumbUrl("", payload))
                .isEqualTo("https://cdn.example.com/thumb.jpg");
        assertThat(AgentWechatDisplayFormatter.extractMediaName("", payload))
                .isEqualTo("quote.jpg");
    }

    @Test
    void extractMediaUrlFindsWechatXmlAttributes() {
        String content = """
                <msg>
                  <img aeskey="k"
                       cdnthumburl="https://cdn.example.com/thumb.jpg"
                       cdnbigimgurl="//cdn.example.com/big.jpg" />
                </msg>
                """;

        assertThat(AgentWechatDisplayFormatter.extractMediaUrl(AgentConstants.MESSAGE_TYPE_IMAGE, content, Map.of()))
                .isEqualTo("https://cdn.example.com/big.jpg");
        assertThat(AgentWechatDisplayFormatter.extractThumbUrl(content, Map.of()))
                .isEqualTo("https://cdn.example.com/thumb.jpg");
    }

    @Test
    void extractEmojiUrlFindsCdnUrlAttribute() {
        String content = "<msg><emoji cdnurl=\"https://cdn.example.com/sticker.gif\" /></msg>";

        assertThat(AgentWechatDisplayFormatter.extractMediaUrl(AgentConstants.MESSAGE_TYPE_EMOJI, content, Map.of()))
                .isEqualTo("https://cdn.example.com/sticker.gif");
    }

    @Test
    void extractEmojiUrlPrefersEncryptedPayloadUrl() {
        Map<String, Object> payload = Map.of("Data", Map.of("emoji", Map.of(
                "cdnurl", "http://weixinf.tc.qq.com/110/20401/stodownload?m=cdn&filekey=1",
                "encrypturl", "http://weixinf.tc.qq.com/110/20402/stodownload?m=encrypt&filekey=2",
                "externurl", "http://weixinf.tc.qq.com/110/20403/stodownload?m=extern&filekey=3"
        )));

        assertThat(AgentWechatDisplayFormatter.extractMediaUrl(AgentConstants.MESSAGE_TYPE_EMOJI, "", payload))
                .isEqualTo("http://weixinf.tc.qq.com/110/20402/stodownload?m=encrypt&filekey=2");
    }

    @Test
    void extractEmojiUrlPrefersEncryptedWechatUrl() {
        String content = """
                <msg><emoji
                  cdnurl="http://weixinf.tc.qq.com/110/20401/stodownload?m=cdn&amp;filekey=1"
                  encrypturl="http://weixinf.tc.qq.com/110/20402/stodownload?m=encrypt&amp;filekey=2"
                  externurl="http://weixinf.tc.qq.com/110/20403/stodownload?m=extern&amp;filekey=3" /></msg>
                """;

        assertThat(AgentWechatDisplayFormatter.extractMediaUrl(AgentConstants.MESSAGE_TYPE_EMOJI, content, Map.of()))
                .isEqualTo("http://weixinf.tc.qq.com/110/20402/stodownload?m=encrypt&filekey=2");
    }

    @Test
    void extractMediaAesKeyFindsPayloadAndXml() {
        Map<String, Object> payload = Map.of("Data", Map.of("emoji", Map.of("aeskey", "payload-key")));
        String content = "<msg><emoji aeskey=\"xml-key\" /></msg>";

        assertThat(AgentWechatDisplayFormatter.extractMediaAesKey(content, payload)).isEqualTo("payload-key");
        assertThat(AgentWechatDisplayFormatter.extractMediaAesKey(content, Map.of())).isEqualTo("xml-key");
    }

    @Test
    void extractVoiceXmlRemovesGroupPrefixAndFindsDuration() {
        String content = "wxid_member:\n<msg><voicemsg voicelength=\"2300\" /></msg>";

        assertThat(AgentWechatDisplayFormatter.extractVoiceXml(content, Map.of()))
                .isEqualTo("<msg><voicemsg voicelength=\"2300\" /></msg>");
        assertThat(AgentWechatDisplayFormatter.extractVoiceDurationMillis(content, Map.of()))
                .isEqualTo(2300);
    }

    @Test
    void extractMessageXmlAndEmojiMd5FromPayloadAndContent() {
        Map<String, Object> payload = Map.of("Data", Map.of("emoji", Map.of("md5", "payload-md5")));
        String content = "member:\n<msg><emoji md5=\"xml-md5\" /></msg>";

        assertThat(AgentWechatDisplayFormatter.extractMessageXml(content, Map.of()))
                .isEqualTo("<msg><emoji md5=\"xml-md5\" /></msg>");
        assertThat(AgentWechatDisplayFormatter.extractEmojiMd5(content, payload))
                .isEqualTo("payload-md5");
        assertThat(AgentWechatDisplayFormatter.extractEmojiMd5(content, Map.of()))
                .isEqualTo("xml-md5");
    }

}
