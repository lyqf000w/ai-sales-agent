package cn.ai.sales.module.agent.service.webhook;

import cn.ai.sales.module.agent.service.gewe.GeweScalarNormalizer;
import cn.ai.sales.module.agent.service.gewe.GeweMessageTimeNormalizer;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.WEBHOOK_PAYLOAD_INVALID;

@Component
public class GeweCallbackParser {

    private static final String[] CHATROOM_ID_KEYS = {
            "chatroomId", "chatRoomId", "chatroomWxid", "chatRoomWxid",
            "roomId", "roomWxid", "roomUserName", "groupId", "groupWxid",
            "groupUserName", "fromGroup", "from_group", "fromGroupWxid", "fromGroupId",
            "fromChatroom", "fromChatroomId", "FromChatroomName",
            "FromGroupUserName", "GroupUserName", "ChatRoomId", "ChatroomId"
    };
    private static final String[] GROUP_MEMBER_WXID_KEYS = {
            "groupMemberWxid", "memberWxid", "memberUserName", "actualUserName", "actualWxid",
            "senderWxid", "fromMemberWxid", "chatroomMemberWxid", "FromMemberUserName",
            "ActualUserName", "ActualWxid", "MemberUserName"
    };
    private static final String[] CONTENT_KEYS = {
            "content", "Content", "text", "Text", "msgContent", "MsgContent", "message", "Message",
            "xml", "Xml", "xmlContent", "XmlContent", "rawContent", "RawContent", "cdnurl", "cdnUrl",
            "url", "Url"
    };

    public GeweCallbackMessage parse(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw exception(WEBHOOK_PAYLOAD_INVALID);
        }
        Map<String, Object> data = readData(payload);
        boolean legacy = data != payload;

        String eventType = StrUtil.blankToDefault(
                getString(payload, false, "eventCode", "event_code", "typeName", "TypeName", "type_name"),
                "message");
        String appId = firstNotBlank(
                getString(payload, false, "appid", "appId", "Appid", "AppId", "app_id"),
                getString(data, false, "appid", "appId", "Appid", "AppId", "app_id"));
        String ownerWxid = firstNotBlank(
                getString(payload, false, "wxid", "Wxid", "WxID"),
                getString(data, false, "wxid", "Wxid", "WxID"));
        String geweMessageId = firstNotBlank(
                getString(payload, false, "newMsgId", "NewMsgId", "msgId", "MsgId"),
                getString(data, false, "newMsgId", "NewMsgId", "NewMsgID", "MsgId", "MsgID", "msgId"));
        String fromUser = firstNotBlank(
                getString(payload, false, "fromUser", "fromWxid", "FromUserName", "from_user"),
                getString(data, false, "fromUser", "FromUser", "FromUserName", "fromUserName", "fromWxid", "from_user"));
        String toUser = firstNotBlank(
                getString(payload, false, "toUser", "toWxid", "ToUserName", "to_user"),
                getString(data, false, "toUser", "ToUser", "ToUserName", "toUserName", "toWxid", "to_user"));
        String content = firstNotBlank(getString(payload, "content", false),
                getString(payload, "Content", false),
                getString(data, "content", false),
                getString(data, "Content", false),
                getString(data, "text", false),
                firstNestedString(payload, CONTENT_KEYS),
                firstNestedString(data, CONTENT_KEYS));
        Integer messageType = toMessageType(firstValue(payload, data, "msgType", "MsgType", "type", "Type",
                "messageType", "MessageType"), content, payload, data);
        Long createTime = Convert.toLong(GeweScalarNormalizer.unwrap(
                firstValue(payload, data, "createTime", "CreateTime", "timestamp")), null);
        boolean selfSent = Convert.toBool(GeweScalarNormalizer.unwrap(firstValue(payload, data, "isSelf", "IsSelf")),
                StrUtil.equals(fromUser, ownerWxid));

        if (StrUtil.isBlank(appId) || StrUtil.isBlank(ownerWxid) || StrUtil.isBlank(geweMessageId)) {
            throw exception(WEBHOOK_PAYLOAD_INVALID);
        }

        String chatroomWxid = firstChatroomId(payload, data, fromUser, toUser);
        String contactWxid = StrUtil.isNotBlank(chatroomWxid) ? chatroomWxid : selfSent ? toUser : fromUser;
        if (StrUtil.isBlank(contactWxid)) {
            throw exception(WEBHOOK_PAYLOAD_INVALID);
        }
        String contactDisplayName = selfSent
                ? firstDisplayName(payload, data, "to")
                : firstDisplayName(payload, data, "from");
        String groupDisplayName = firstNestedString(payload, "roomName", "chatroomName", "chatRoomName",
                "groupName", "groupNickName", "groupNickname", "fromGroupName", "FromGroupName",
                "roomNickName", "chatroomNickName", "RoomName", "ChatRoomName", "GroupName", "GroupNickName");
        String groupMemberWxid = StrUtil.isNotBlank(chatroomWxid) && !selfSent
                ? firstGroupMemberWxid(payload, data, content, fromUser) : null;
        String groupMemberDisplayName = StrUtil.isNotBlank(chatroomWxid) && !selfSent
                ? firstNestedString(payload, "groupMemberNickName", "groupMemberNickname", "memberNickName",
                "memberNickname", "actualNickName", "actualNickname", "senderNickName", "senderNickname",
                "fromUserNickName", "fromNickname", "FromMemberNickName", "ActualNickName", "MemberNickName")
                : null;

        return new GeweCallbackMessage(
                appId + ":" + eventType + ":" + contactWxid + ":" + geweMessageId,
                eventType,
                appId,
                ownerWxid,
                contactWxid,
                geweMessageId,
                messageType,
                content,
                contactDisplayName,
                groupDisplayName,
                groupMemberWxid,
                groupMemberDisplayName,
                toMessageTime(createTime),
                selfSent);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readData(Map<String, Object> payload) {
        Object data = payload.get("Data");
        if (!(data instanceof Map<?, ?>)) {
            data = payload.get("data");
        }
        return data instanceof Map<?, ?> map ? (Map<String, Object>) map : payload;
    }

    private String getString(Map<String, Object> source, String key, boolean required) {
        String value = GeweScalarNormalizer.toWechatString(source.get(key));
        if (required && StrUtil.isBlank(value)) {
            throw exception(WEBHOOK_PAYLOAD_INVALID);
        }
        return value;
    }

    private Object firstValue(Map<String, Object> payload, Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return value;
            }
            value = data.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String getString(Map<String, Object> source, boolean required, String... keys) {
        for (String key : keys) {
            String value = GeweScalarNormalizer.toWechatString(source.get(key));
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        if (required) {
            throw exception(WEBHOOK_PAYLOAD_INVALID);
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String firstDisplayName(Map<String, Object> payload, Map<String, Object> data, String side) {
        if ("from".equals(side)) {
            return firstNotBlank(
                    getString(payload, false, "fromUserNickName", "fromNickname", "fromNickName", "senderNickName", "senderName"),
                    getString(data, false, "FromNickName", "fromNickName", "fromNickname", "SenderNickName", "senderName"));
        }
        return firstNotBlank(
                getString(payload, false, "toUserNickName", "toNickname", "toNickName", "receiverNickName", "receiverName"),
                getString(data, false, "ToNickName", "toNickName", "toNickname", "ReceiverNickName", "receiverName"));
    }

    @SuppressWarnings("unchecked")
    private String firstNestedString(Object source, String... keys) {
        if (source instanceof Map<?, ?> map) {
            for (String key : keys) {
                Object value = map.get(key);
                String text = GeweScalarNormalizer.toWechatString(value);
                if (StrUtil.isNotBlank(text)) {
                    return text;
                }
            }
            for (Object value : map.values()) {
                String found = firstNestedString(value, keys);
                if (StrUtil.isNotBlank(found)) {
                    return found;
                }
            }
        } else if (source instanceof Collection<?> collection) {
            for (Object value : collection) {
                String found = firstNestedString(value, keys);
                if (StrUtil.isNotBlank(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    private String firstChatroomId(Map<String, Object> payload, Map<String, Object> data, String... candidates) {
        for (String candidate : candidates) {
            String text = GeweScalarNormalizer.cleanText(candidate);
            if (StrUtil.endWith(text, "@chatroom")) {
                return text;
            }
        }
        String fromPayload = firstNestedChatroomId(payload);
        if (StrUtil.isNotBlank(fromPayload)) {
            return fromPayload;
        }
        return firstNestedChatroomId(data);
    }

    private String firstGroupMemberWxid(Map<String, Object> payload, Map<String, Object> data, String content,
                                        String fromUser) {
        String fromPayload = firstNestedWxid(payload, GROUP_MEMBER_WXID_KEYS);
        if (StrUtil.isNotBlank(fromPayload)) {
            return fromPayload;
        }
        String fromData = firstNestedWxid(data, GROUP_MEMBER_WXID_KEYS);
        if (StrUtil.isNotBlank(fromData)) {
            return fromData;
        }
        String fromContent = extractGroupMemberWxidFromContent(content);
        if (StrUtil.isNotBlank(fromContent)) {
            return fromContent;
        }
        String cleanedFromUser = GeweScalarNormalizer.cleanText(fromUser);
        return GeweScalarNormalizer.isRawWechatIdentifier(cleanedFromUser) ? cleanedFromUser : null;
    }

    private String extractGroupMemberWxidFromContent(String content) {
        String cleaned = GeweScalarNormalizer.cleanText(content);
        if (StrUtil.isBlank(cleaned)) {
            return null;
        }
        int separator = cleaned.indexOf(":\n");
        if (separator <= 0) {
            separator = cleaned.indexOf(":\\n");
        }
        if (separator <= 0) {
            return null;
        }
        String candidate = cleaned.substring(0, separator).trim();
        return GeweScalarNormalizer.isRawWechatIdentifier(candidate) ? candidate : null;
    }

    @SuppressWarnings("unchecked")
    private String firstNestedWxid(Object source, String... keys) {
        if (source instanceof Map<?, ?> map) {
            for (String key : keys) {
                Object value = map.get(key);
                String text = GeweScalarNormalizer.toWechatString(value);
                if (GeweScalarNormalizer.isRawWechatIdentifier(text)) {
                    return text;
                }
            }
            for (Object value : map.values()) {
                String found = firstNestedWxid(value, keys);
                if (StrUtil.isNotBlank(found)) {
                    return found;
                }
            }
        } else if (source instanceof Collection<?> collection) {
            for (Object value : collection) {
                String found = firstNestedWxid(value, keys);
                if (StrUtil.isNotBlank(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String firstNestedChatroomId(Object source) {
        if (source instanceof Map<?, ?> map) {
            for (String key : CHATROOM_ID_KEYS) {
                Object value = map.get(key);
                String text = GeweScalarNormalizer.toWechatString(value);
                if (StrUtil.endWith(text, "@chatroom")) {
                    return text;
                }
            }
            for (Object value : map.values()) {
                String found = firstNestedChatroomId(value);
                if (StrUtil.isNotBlank(found)) {
                    return found;
                }
            }
        } else if (source instanceof Collection<?> collection) {
            for (Object value : collection) {
                String found = firstNestedChatroomId(value);
                if (StrUtil.isNotBlank(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    private Integer toMessageType(Object value, String content, Map<String, Object> payload, Map<String, Object> data) {
        Integer numericType = Convert.toInt(GeweScalarNormalizer.unwrap(value), null);
        if (numericType != null) {
            return numericType;
        }
        String textType = Convert.toStr(GeweScalarNormalizer.unwrap(value), "");
        Integer namedType = switch (textType.toUpperCase()) {
            case "TEXT" -> 1;
            case "IMAGE" -> 3;
            case "VOICE" -> 34;
            case "VIDEO" -> 43;
            case "EMOJI", "EMOTICON" -> 47;
            case "APP", "APPMSG", "LINK", "MINI_PROGRAM" -> 49;
            case "SYSTEM" -> 10000;
            default -> null;
        };
        return namedType != null ? namedType : inferMessageType(content, payload, data);
    }

    private Integer inferMessageType(String content, Map<String, Object> payload, Map<String, Object> data) {
        String cleaned = GeweScalarNormalizer.cleanText(content);
        String lowerContent = StrUtil.blankToDefault(cleaned, "").toLowerCase();
        if (StrUtil.containsAny(lowerContent, "<emoji", "cdnurl=\"", "cdnurl='")) {
            return 47;
        }
        if (StrUtil.containsAny(lowerContent, "<img", "cdnbigimgurl=", "cdnmidimgurl=", "cdnthumburl=")) {
            return 3;
        }
        if (StrUtil.containsAny(lowerContent, "<videomsg", "<video", "cdnvideourl=", "videourl=")) {
            return 43;
        }
        if (StrUtil.containsAny(lowerContent, "<appmsg", "<appattach")) {
            return 49;
        }
        String mediaKey = firstNestedKey(payload, data, "emoji", "cdnurl", "cdnbigimgurl", "cdnmidimgurl",
                "cdnthumburl", "imageUrl", "imgUrl", "videoUrl", "cdnvideourl", "fileUrl", "downloadUrl");
        if (StrUtil.equalsAnyIgnoreCase(mediaKey, "emoji")) {
            return 47;
        }
        if (StrUtil.equalsAnyIgnoreCase(mediaKey, "videoUrl", "cdnvideourl")) {
            return 43;
        }
        if (StrUtil.equalsAnyIgnoreCase(mediaKey, "fileUrl", "downloadUrl")) {
            return 49;
        }
        if (StrUtil.isNotBlank(mediaKey)) {
            return 3;
        }
        return 0;
    }

    private String firstNestedKey(Object first, Object second, String... keys) {
        String key = firstNestedKey(first, keys);
        return StrUtil.isNotBlank(key) ? key : firstNestedKey(second, keys);
    }

    private String firstNestedKey(Object source, String... keys) {
        if (source instanceof Map<?, ?> map) {
            for (String key : keys) {
                if (map.containsKey(key) && map.get(key) != null) {
                    return key;
                }
            }
            for (Object value : map.values()) {
                String found = firstNestedKey(value, keys);
                if (StrUtil.isNotBlank(found)) {
                    return found;
                }
            }
        } else if (source instanceof Collection<?> collection) {
            for (Object value : collection) {
                String found = firstNestedKey(value, keys);
                if (StrUtil.isNotBlank(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    private LocalDateTime toMessageTime(Long createTime) {
        return GeweMessageTimeNormalizer.fromEpoch(createTime);
    }

}
