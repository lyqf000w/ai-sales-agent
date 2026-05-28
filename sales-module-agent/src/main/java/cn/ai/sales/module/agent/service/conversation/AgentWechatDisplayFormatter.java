package cn.ai.sales.module.agent.service.conversation;

import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.gewe.GeweScalarNormalizer;
import cn.hutool.core.util.StrUtil;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AgentWechatDisplayFormatter {

    private static final String[] SENDER_NAME_KEYS = {
            "groupMemberDisplayName", "groupMemberNickName", "groupMemberNickname",
            "memberDisplayName", "memberNickName", "memberNickname",
            "actualNickName", "actualNickname", "senderNickName", "senderNickname",
            "fromUserNickName", "fromNickname", "fromNickName", "FromMemberNickName",
            "ActualNickName", "MemberNickName"
    };
    private static final String[] MEDIA_URL_KEYS = {
            "mediaUrl", "media_url", "fileUrl", "file_url", "downloadUrl", "download_url",
            "cdnUrl", "cdnurl", "cdn_url", "cdnAttachUrl", "cdnAttachurl", "attachUrl",
            "encryptUrl", "encrypturl", "externUrl", "externurl",
            "cdnBigImgUrl", "cdnbigimgurl", "cdnMidImgUrl", "cdnmidimgurl",
            "imageUrl", "image_url", "imgUrl", "img_url", "picUrl", "pic_url",
            "videoUrl", "video_url", "cdnVideoUrl", "cdnvideourl", "url", "Url", "URL"
    };
    private static final String[] EMOJI_MEDIA_URL_KEYS = {
            "encryptUrl", "encrypturl", "externUrl", "externurl",
            "cdnUrl", "cdnurl", "cdn_url", "url", "Url", "URL",
            "thumbUrl", "thumb_url", "cdnThumbUrl", "cdnthumburl", "cdn_thumb_url"
    };
    private static final String[] THUMB_URL_KEYS = {
            "thumbUrl", "thumb_url", "thumbnailUrl", "thumbnail_url", "cdnThumbUrl",
            "cdnthumburl", "cdn_thumb_url", "coverUrl", "cover_url", "previewUrl",
            "preview_url", "cdnThumbImgUrl", "cdnthumbimgurl", "thumb", "thumbnail"
    };
    private static final String[] MEDIA_NAME_KEYS = {
            "fileName", "file_name", "filename", "title", "name", "FileName"
    };
    private static final String[] MEDIA_AES_KEY_KEYS = {
            "aesKey", "aeskey", "AesKey", "AESKey", "cdnAesKey", "cdnaeskey", "encryptKey", "encryptkey"
    };
    private static final String[] XML_CONTENT_KEYS = {
            "xml", "Xml", "xmlContent", "XmlContent", "content", "Content", "msgContent", "MsgContent",
            "rawContent", "RawContent"
    };

    private AgentWechatDisplayFormatter() {
    }

    public static String cleanScalar(String value) {
        return GeweScalarNormalizer.cleanText(value);
    }

    public static String resolveContactDisplayName(Long contactId, String wxid, String... names) {
        String name = firstUsableName(wxid, names);
        return StrUtil.blankToDefault(name, "\u5ba2\u6237 #" + contactId);
    }

    public static String firstUsableName(String wxid, String... names) {
        for (String name : names) {
            if (isUsableName(name, wxid)) {
                return GeweScalarNormalizer.cleanText(name);
            }
        }
        return null;
    }

    public static boolean isUsableName(String name, String wxid) {
        String text = GeweScalarNormalizer.cleanText(name);
        return StrUtil.isNotBlank(text)
                && !StrUtil.equals(text, wxid)
                && !GeweScalarNormalizer.isRawWechatIdentifier(text);
    }

    public static String formatMessageContent(Integer messageType, String content) {
        String cleaned = cleanScalar(content);
        if (StrUtil.isBlank(cleaned)) {
            return "";
        }
        if (isXmlPayload(cleaned)) {
            String summary = firstNotBlank(extractXmlTag(cleaned, "title"),
                    extractXmlTag(cleaned, "des"),
                    extractXmlTag(cleaned, "summary"),
                    extractXmlTag(cleaned, "appname"));
            if (StrUtil.isNotBlank(summary)) {
                return summary;
            }
        }
        if (messageType == null || messageType == AgentConstants.MESSAGE_TYPE_TEXT) {
            return cleaned;
        }
        if (messageType == AgentConstants.MESSAGE_TYPE_IMAGE) {
            return "\u005b\u56fe\u7247\u005d";
        }
        if (messageType == AgentConstants.MESSAGE_TYPE_VOICE) {
            return "\u005b\u8bed\u97f3\u005d";
        }
        if (messageType == AgentConstants.MESSAGE_TYPE_VIDEO) {
            return "\u005b\u89c6\u9891\u005d";
        }
        if (messageType == AgentConstants.MESSAGE_TYPE_EMOJI) {
            return "\u005b\u8868\u60c5\u005d";
        }
        if (messageType == AgentConstants.MESSAGE_TYPE_FILE_OR_LINK) {
            return "\u005b\u94fe\u63a5/\u5c0f\u7a0b\u5e8f/\u6587\u4ef6\u005d";
        }
        return "\u005b\u975e\u6587\u672c\u6d88\u606f\u005d";
    }

    public static String resolveSenderDisplayName(String content, Map<String, Object> rawPayload, String memberWxid) {
        String fromPrefix = extractGroupMemberDisplayNamePrefix(content);
        if (StrUtil.isNotBlank(fromPrefix)) {
            return fromPrefix;
        }
        return firstUsableName(memberWxid, firstNestedString(rawPayload, SENDER_NAME_KEYS));
    }

    public static boolean isGroupMessage(String content, Map<String, Object> rawPayload) {
        return StrUtil.isNotBlank(extractGroupMemberWxidPrefix(content)) || containsChatroomId(rawPayload);
    }

    public static String stripGroupSenderPrefix(String content, String senderDisplayName) {
        String cleaned = cleanScalar(content);
        if (StrUtil.isBlank(cleaned)) {
            return cleaned;
        }
        int separator = groupPrefixSeparator(cleaned);
        if (separator <= 0) {
            return cleaned;
        }
        String prefix = cleaned.substring(0, separator).trim();
        if (GeweScalarNormalizer.isRawWechatIdentifier(prefix)
                || (StrUtil.isNotBlank(senderDisplayName) && StrUtil.equals(prefix, senderDisplayName))) {
            int messageStart = cleaned.startsWith(":\\n", separator) ? separator + 3 : separator + 2;
            return cleaned.substring(messageStart);
        }
        return cleaned;
    }

    public static String extractMediaUrl(Integer messageType, String content, Map<String, Object> rawPayload) {
        if (messageType != null && messageType == AgentConstants.MESSAGE_TYPE_EMOJI) {
            String fromPayload = firstNestedUrl(rawPayload, EMOJI_MEDIA_URL_KEYS);
            if (StrUtil.isNotBlank(fromPayload)) {
                return fromPayload;
            }
            String cleaned = cleanScalar(content);
            return firstRenderableUrl(extractXmlValue(cleaned, "encrypturl"), extractXmlValue(cleaned, "externurl"),
                    extractXmlValue(cleaned, "cdnurl"), extractXmlValue(cleaned, "url"),
                    extractXmlValue(cleaned, "thumburl"));
        }
        String fromPayload = firstNestedUrl(rawPayload, MEDIA_URL_KEYS);
        if (StrUtil.isNotBlank(fromPayload)) {
            return fromPayload;
        }
        String cleaned = cleanScalar(content);
        return firstRenderableUrl(extractXmlValue(cleaned, "cdnurl"), extractXmlValue(cleaned, "cdnbigimgurl"),
                extractXmlValue(cleaned, "cdnmidimgurl"), extractXmlValue(cleaned, "url"),
                extractXmlValue(cleaned, "videourl"), extractXmlValue(cleaned, "cdnvideourl"));
    }

    public static String extractThumbUrl(String content, Map<String, Object> rawPayload) {
        String fromPayload = firstNestedUrl(rawPayload, THUMB_URL_KEYS);
        if (StrUtil.isNotBlank(fromPayload)) {
            return fromPayload;
        }
        String cleaned = cleanScalar(content);
        return firstRenderableUrl(extractXmlValue(cleaned, "cdnthumburl"), extractXmlValue(cleaned, "cdnthumbimgurl"),
                extractXmlValue(cleaned, "thumburl"), extractXmlValue(cleaned, "coverurl"));
    }

    public static String extractMediaName(String content, Map<String, Object> rawPayload) {
        String fromPayload = firstNestedString(rawPayload, MEDIA_NAME_KEYS);
        if (StrUtil.isNotBlank(fromPayload)) {
            return fromPayload;
        }
        String cleaned = cleanScalar(content);
        return firstNotBlank(extractXmlTag(cleaned, "title"), extractXmlTag(cleaned, "filename"),
                extractXmlValue(cleaned, "filename"));
    }

    public static String extractMediaAesKey(String content, Map<String, Object> rawPayload) {
        String fromPayload = firstNestedString(rawPayload, MEDIA_AES_KEY_KEYS);
        if (StrUtil.isNotBlank(fromPayload)) {
            return fromPayload;
        }
        return firstNotBlank(extractXmlValue(cleanScalar(content), "aeskey"),
                extractXmlValue(cleanScalar(content), "aesKey"));
    }

    public static String extractMessageXml(String content, Map<String, Object> rawPayload) {
        String fromPayload = firstNestedString(rawPayload, XML_CONTENT_KEYS);
        String xml = extractXmlDocument(fromPayload);
        if (StrUtil.isNotBlank(xml)) {
            return xml;
        }
        return extractXmlDocument(content);
    }

    public static String extractEmojiMd5(String content, Map<String, Object> rawPayload) {
        String fromPayload = firstNestedString(rawPayload, "emojiMd5", "emoji_md5", "md5", "Md5", "MD5");
        if (StrUtil.isNotBlank(fromPayload)) {
            return fromPayload;
        }
        String xml = extractMessageXml(content, rawPayload);
        return firstNotBlank(extractXmlValue(xml, "md5"), extractXmlValue(xml, "emojiMd5"));
    }

    public static String extractVoiceXml(String content, Map<String, Object> rawPayload) {
        String xml = extractMessageXml(content, rawPayload);
        if (StrUtil.isNotBlank(xml) && StrUtil.containsIgnoreCase(xml, "<voicemsg")) {
            return xml;
        }
        return "";
    }

    public static Integer extractVoiceDurationMillis(String content, Map<String, Object> rawPayload) {
        String xml = extractVoiceXml(content, rawPayload);
        String duration = firstNotBlank(extractXmlValue(xml, "voicelength"),
                extractXmlValue(xml, "voiceLength"),
                extractXmlValue(xml, "playlength"),
                extractXmlValue(xml, "playLength"));
        if (StrUtil.isBlank(duration) || !duration.matches("\\d+")) {
            return null;
        }
        int value = Integer.parseInt(duration);
        return value < 1000 ? value * 1000 : value;
    }

    public static String extractGroupMemberWxidPrefix(String content) {
        String cleaned = cleanScalar(content);
        if (StrUtil.isBlank(cleaned)) {
            return null;
        }
        int separator = groupPrefixSeparator(cleaned);
        if (separator <= 0) {
            return null;
        }
        String candidate = cleaned.substring(0, separator).trim();
        return GeweScalarNormalizer.isRawWechatIdentifier(candidate) ? candidate : null;
    }

    public static String replaceGroupMemberWxidPrefix(String content, String displayName) {
        String cleaned = cleanScalar(content);
        if (StrUtil.isBlank(cleaned)) {
            return cleaned;
        }
        int separator = groupPrefixSeparator(cleaned);
        if (separator <= 0) {
            return cleaned;
        }
        String candidate = cleaned.substring(0, separator).trim();
        if (!GeweScalarNormalizer.isRawWechatIdentifier(candidate)) {
            return cleaned;
        }
        String name = firstUsableName(candidate, displayName);
        if (StrUtil.isBlank(name)) {
            name = "\u7fa4\u6210\u5458";
        }
        int messageStart = cleaned.startsWith(":\\n", separator) ? separator + 3 : separator + 2;
        return name + ":\n" + cleaned.substring(messageStart);
    }

    private static boolean isXmlPayload(String content) {
        return StrUtil.startWith(content, "<") && StrUtil.endWith(content, ">");
    }

    private static String extractXmlTag(String content, String tagName) {
        String text = StrUtil.subBetween(content, "<" + tagName + ">", "</" + tagName + ">");
        if (StrUtil.isBlank(text)) {
            return "";
        }
        return cleanScalar(unescapeXmlText(text).trim());
    }

    private static String extractXmlValue(String content, String name) {
        String fromTag = extractXmlTag(content, name);
        if (StrUtil.isNotBlank(fromTag)) {
            return fromTag;
        }
        return extractXmlAttribute(content, name);
    }

    private static String extractXmlAttribute(String content, String attributeName) {
        String cleaned = unescapeXmlText(cleanScalar(content));
        if (StrUtil.isBlank(cleaned)) {
            return "";
        }
        Matcher matcher = Pattern.compile("(?i)\\b" + Pattern.quote(attributeName) + "\\s*=\\s*(['\"])(.*?)\\1")
                .matcher(cleaned);
        if (!matcher.find()) {
            return "";
        }
        return cleanScalar(unescapeXmlText(matcher.group(2)).trim());
    }

    private static String extractXmlDocument(String value) {
        String cleaned = unescapeXmlText(cleanScalar(value));
        if (StrUtil.isBlank(cleaned)) {
            return "";
        }
        int start = cleaned.indexOf("<msg");
        if (start < 0) {
            return "";
        }
        int end = cleaned.lastIndexOf("</msg>");
        return end >= start ? cleaned.substring(start, end + "</msg>".length()).trim() : cleaned.substring(start).trim();
    }

    private static String unescapeXmlText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static int groupPrefixSeparator(String content) {
        int separator = content.indexOf(":\n");
        if (separator > 0) {
            return separator;
        }
        return content.indexOf(":\\n");
    }

    private static String extractGroupMemberDisplayNamePrefix(String content) {
        String cleaned = cleanScalar(content);
        if (StrUtil.isBlank(cleaned)) {
            return null;
        }
        int separator = groupPrefixSeparator(cleaned);
        if (separator <= 0) {
            return null;
        }
        String candidate = cleaned.substring(0, separator).trim();
        return GeweScalarNormalizer.isRawWechatIdentifier(candidate) ? null : candidate;
    }

    @SuppressWarnings("unchecked")
    private static String firstNestedString(Object source, String... keys) {
        if (source instanceof Map<?, ?> map) {
            for (String key : keys) {
                Object value = map.get(key);
                String text = cleanScalar(value == null ? null : String.valueOf(value));
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

    private static String firstNestedUrl(Object source, String... keys) {
        if (source instanceof Map<?, ?> map) {
            for (String key : keys) {
                Object value = map.get(key);
                String text = cleanScalar(value == null ? null : String.valueOf(value));
                String normalized = normalizeRenderableUrl(text);
                if (StrUtil.isNotBlank(normalized)) {
                    return normalized;
                }
            }
            for (Object value : map.values()) {
                String found = firstNestedUrl(value, keys);
                if (StrUtil.isNotBlank(found)) {
                    return found;
                }
            }
        } else if (source instanceof Collection<?> collection) {
            for (Object value : collection) {
                String found = firstNestedUrl(value, keys);
                if (StrUtil.isNotBlank(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String firstRenderableUrl(String... values) {
        for (String value : values) {
            String normalized = normalizeRenderableUrl(value);
            if (StrUtil.isNotBlank(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private static String normalizeRenderableUrl(String value) {
        String text = cleanScalar(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        text = unescapeXmlText(text).trim();
        if (StrUtil.startWith(text, "//")) {
            text = "https:" + text;
        }
        return isRenderableUrl(text) ? text : null;
    }

    private static boolean isRenderableUrl(String value) {
        String text = cleanScalar(value);
        return StrUtil.startWithIgnoreCase(text, "http://")
                || StrUtil.startWithIgnoreCase(text, "https://")
                || StrUtil.startWithIgnoreCase(text, "data:image/");
    }

    private static boolean containsChatroomId(Object source) {
        if (source instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                if (containsChatroomId(value)) {
                    return true;
                }
            }
        } else if (source instanceof Collection<?> collection) {
            for (Object value : collection) {
                if (containsChatroomId(value)) {
                    return true;
                }
            }
        } else if (source != null) {
            return StrUtil.endWith(cleanScalar(String.valueOf(source)), "@chatroom");
        }
        return false;
    }

}
