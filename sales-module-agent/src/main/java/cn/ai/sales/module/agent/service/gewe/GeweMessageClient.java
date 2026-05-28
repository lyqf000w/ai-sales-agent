package cn.ai.sales.module.agent.service.gewe;

import cn.ai.sales.framework.common.util.http.HttpUtils;
import cn.ai.sales.framework.common.util.json.JsonUtils;
import cn.ai.sales.module.agent.dal.dataobject.AgentGeweCredentialDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.mysql.AgentGeweCredentialMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GeweMessageClient {

    private static final String TOKEN_HEADER = "X-GEWE-TOKEN";
    private static final String LOGIN_REGION_ID_BEIJING = "110000";
    private static final int MEDIA_CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int MEDIA_READ_TIMEOUT_MILLIS = 20_000;
    private static final int MAX_MEDIA_BYTES = 12 * 1024 * 1024;

    @Resource
    private AgentGeweCredentialMapper credentialMapper;

    public boolean canSend(AgentWechatAccountDO account) {
        return resolveEndpoint(account) != null;
    }

    public GeweLoginQrCodeResult getLoginQrCode(AgentGeweCredentialDO credential) {
        Map<String, Object> body = new HashMap<>();
        body.put("appId", "");
        body.put("proxyIp", "");
        body.put("regionId", LOGIN_REGION_ID_BEIJING);
        body.put("type", "mac");
        body.put("aid", "");

        Map<String, Object> response = post(credential, "/login/getLoginQrCode", body);
        Map<String, Object> data = readDataMap(response);
        String appId = readString(data, "appId", "appid", "AppId");
        String qrData = readString(data, "qrData", "qrUrl", "qrcodeUrl", "qrCodeUrl", "url");
        String uuid = StrUtil.blankToDefault(readString(data, "uuid", "qrUuid", "qrcodeUuid", "Uuid"),
                extractUuidFromQrData(qrData));
        String qrImgBase64 = readString(data, "qrImgBase64", "qrCodeBase64", "qrcodeBase64", "qrImg");
        return new GeweLoginQrCodeResult(appId, uuid, qrData, qrImgBase64, response);
    }

    public GeweLoginCheckResult checkLogin(AgentGeweCredentialDO credential, String appId, String uuid) {
        Map<String, Object> body = new HashMap<>();
        body.put("appId", appId);
        body.put("proxyIp", "");
        body.put("uuid", uuid);
        body.put("autoSliding", false);

        Map<String, Object> response = post(credential, "/login/checkLogin", body);
        Map<String, Object> data = readDataMap(response);
        Map<String, Object> loginInfo = readMap(data, "loginInfo", "LoginInfo", "acctSectResp", "accountInfo",
                "profile");
        Integer status = firstInt(data, loginInfo, "status", "loginStatus", "state", "code");
        String statusText = firstString(data, loginInfo, "status", "loginStatus", "state", "statusText",
                "statusName", "message", "msg");
        boolean success = isLoginSuccess(data, loginInfo, status, statusText);
        String verifyUrl = firstString(data, loginInfo, "url", "verifyUrl", "confirmUrl", "authUrl",
                "redirectUrl");
        String nickName = firstString(data, loginInfo, "nickName", "nickname", "NickName");
        String avatar = firstString(data, loginInfo, "headImgUrl", "avatar", "bigHeadImgUrl", "smallHeadImgUrl");
        boolean waitConfirm = !success && isWaitingConfirm(status, statusText, verifyUrl, nickName, avatar);
        return new GeweLoginCheckResult(success, waitConfirm, verifyUrl, nickName, avatar, response);
    }

    public GeweProfileResult getProfile(AgentGeweCredentialDO credential, String appId) {
        Map<String, Object> body = new HashMap<>();
        body.put("appId", appId);
        body.put("proxyIp", "");

        Map<String, Object> response = post(credential, "/personal/getProfile", body);
        Map<String, Object> data = readDataMap(response);
        Map<String, Object> profile = readMap(data, "profile", "userInfo", "loginInfo", "accountInfo");
        String avatar = StrUtil.blankToDefault(firstString(data, profile, "bigHeadImgUrl", "bigHeadUrl", "avatar"),
                firstString(data, profile, "smallHeadImgUrl", "smallHeadUrl", "headImgUrl"));
        return new GeweProfileResult(firstString(data, profile, "wxid", "userName", "username"),
                firstString(data, profile, "nickName", "nickname", "name"), avatar,
                firstString(data, profile, "mobile", "phone"), response);
    }

    public Boolean checkOnline(AgentWechatAccountDO account) {
        GeweEndpoint endpoint = resolveEndpoint(account);
        if (endpoint == null || StrUtil.isBlank(account.getGeweAppId())) {
            return null;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("appId", account.getGeweAppId());

        try {
            Map<String, Object> response = post(endpoint.baseUrl(), endpoint.token(), "/login/checkOnline", body);
            if (!isSuccess(response)) {
                log.warn("[checkOnline][accountId({}) appId({}) failed: {}]", account.getId(),
                        account.getGeweAppId(), response.get("msg"));
                return null;
            }
            Object data = response.get("data");
            if (data instanceof Boolean online) {
                return online;
            }
            if (data == null) {
                return null;
            }
            return Boolean.valueOf(String.valueOf(data));
        } catch (Exception ex) {
            log.warn("[checkOnline][accountId({}) appId({}) failed]", account.getId(), account.getGeweAppId(), ex);
            return null;
        }
    }

    public boolean setCallback(AgentGeweCredentialDO credential, String callbackUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("token", credential.getGeweToken());
        body.put("callbackUrl", callbackUrl);
        return isSuccess(post(credential, "/login/setCallback", body));
    }

    public GeweTextSendResult sendText(AgentWechatAccountDO account, String toWxid, String content) {
        GeweEndpoint endpoint = resolveEndpoint(account);
        if (endpoint == null) {
            return GeweTextSendResult.failure("微信账号未配置 Gewe 发送地址或 Token", null);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("appId", account.getGeweAppId());
        body.put("toWxid", toWxid);
        body.put("content", content);

        try {
            Map<String, Object> response = post(endpoint.baseUrl(), endpoint.token(), "/message/postText", body);
            if (isSuccess(response)) {
                return GeweTextSendResult.success(readNewMsgId(response), response);
            }
            return GeweTextSendResult.failure(String.valueOf(response.getOrDefault("msg", "Gewe 发送失败")), response);
        } catch (Exception ex) {
            log.warn("[sendText][accountId({}) toWxid({}) failed]", account.getId(), toWxid, ex);
            return GeweTextSendResult.failure(StrUtil.maxLength(ex.getMessage(), 255), null);
        }
    }

    public GeweVoiceDownloadResult downloadVoice(AgentWechatAccountDO account, String msgId, String xml) {
        GeweEndpoint endpoint = resolveEndpoint(account);
        if (endpoint == null) {
            return GeweVoiceDownloadResult.failure("微信账号未配置 Gewe 下载地址或 Token", null);
        }
        if (StrUtil.isBlank(account.getGeweAppId()) || StrUtil.isBlank(msgId) || StrUtil.isBlank(xml)) {
            return GeweVoiceDownloadResult.failure("语音下载参数不完整", null);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("appId", account.getGeweAppId());
        body.put("msgId", normalizeMessageId(msgId));
        body.put("xml", xml);

        try {
            Map<String, Object> response = post(endpoint.baseUrl(), endpoint.token(), "/message/downloadVoice", body);
            if (!isSuccess(response)) {
                return GeweVoiceDownloadResult.failure(String.valueOf(response.getOrDefault("msg", "Gewe 语音下载失败")),
                        response);
            }
            Map<String, Object> data = readDataMap(response);
            String fileUrl = readString(data, "fileUrl", "url", "downloadUrl");
            if (StrUtil.isBlank(fileUrl)) {
                return GeweVoiceDownloadResult.failure("Gewe 未返回语音文件地址", response);
            }
            return GeweVoiceDownloadResult.success(fileUrl, response);
        } catch (Exception ex) {
            log.warn("[downloadVoice][accountId({}) msgId({}) failed]", account.getId(), msgId, ex);
            return GeweVoiceDownloadResult.failure(StrUtil.maxLength(ex.getMessage(), 255), null);
        }
    }

    public GeweMediaDownloadResult downloadImage(AgentWechatAccountDO account, String xml) {
        GeweEndpoint endpoint = resolveEndpoint(account);
        if (endpoint == null) {
            return GeweMediaDownloadResult.failure("微信账号未配置 Gewe 下载地址或 Token", null);
        }
        if (StrUtil.isBlank(account.getGeweAppId()) || StrUtil.isBlank(xml)) {
            return GeweMediaDownloadResult.failure("图片下载参数不完整", null);
        }
        GeweMediaDownloadResult lastFailure = null;
        for (Integer imageType : List.of(2, 1, 3)) {
            Map<String, Object> body = new HashMap<>();
            body.put("appId", account.getGeweAppId());
            body.put("type", imageType);
            body.put("xml", xml);
            GeweMediaDownloadResult result = downloadMediaUrl(endpoint, "/message/downloadImage", body,
                    "Gewe 图片下载失败");
            if (result.success()) {
                return result;
            }
            lastFailure = result;
        }
        return lastFailure == null ? GeweMediaDownloadResult.failure("Gewe 图片下载失败", null) : lastFailure;
    }

    public GeweMediaDownloadResult downloadEmoji(AgentWechatAccountDO account, String emojiMd5) {
        GeweEndpoint endpoint = resolveEndpoint(account);
        if (endpoint == null) {
            return GeweMediaDownloadResult.failure("微信账号未配置 Gewe 下载地址或 Token", null);
        }
        if (StrUtil.isBlank(account.getGeweAppId()) || StrUtil.isBlank(emojiMd5)) {
            return GeweMediaDownloadResult.failure("表情下载参数不完整", null);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("appId", account.getGeweAppId());
        body.put("emojiMd5", emojiMd5);
        return downloadMediaUrl(endpoint, "/message/downloadEmojiMd5", body, "Gewe 表情下载失败");
    }

    public GeweMediaDownloadResult downloadFile(AgentWechatAccountDO account, String xml) {
        GeweEndpoint endpoint = resolveEndpoint(account);
        if (endpoint == null) {
            return GeweMediaDownloadResult.failure("微信账号未配置 Gewe 下载地址或 Token", null);
        }
        if (StrUtil.isBlank(account.getGeweAppId()) || StrUtil.isBlank(xml)) {
            return GeweMediaDownloadResult.failure("文件下载参数不完整", null);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("appId", account.getGeweAppId());
        body.put("xml", xml);
        return downloadMediaUrl(endpoint, "/message/downloadFile", body, "Gewe 文件下载失败");
    }

    public DownloadedMedia downloadMediaFile(String fileUrl) throws IOException {
        URI uri = URI.create(fileUrl);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("Unsupported media file url");
        }
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(MEDIA_CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(MEDIA_READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Failed to download media file: " + status);
            }
            try (InputStream inputStream = connection.getInputStream()) {
                return new DownloadedMedia(readLimited(inputStream), resolveMediaContentType(fileUrl,
                        connection.getContentType()));
            }
        } finally {
            connection.disconnect();
        }
    }

    private GeweMediaDownloadResult downloadMediaUrl(GeweEndpoint endpoint, String path, Map<String, Object> body,
                                                     String defaultErrorMessage) {
        try {
            Map<String, Object> response = post(endpoint.baseUrl(), endpoint.token(), path, body);
            if (!isSuccess(response)) {
                return GeweMediaDownloadResult.failure(String.valueOf(response.getOrDefault("msg", defaultErrorMessage)),
                        response);
            }
            Map<String, Object> data = readDataMap(response);
            String fileUrl = readString(data, "fileUrl", "url", "downloadUrl");
            if (StrUtil.isBlank(fileUrl)) {
                return GeweMediaDownloadResult.failure("Gewe 未返回媒体文件地址", response);
            }
            return GeweMediaDownloadResult.success(fileUrl, response);
        } catch (Exception ex) {
            log.warn("[downloadMediaUrl][path({}) failed]", path, ex);
            return GeweMediaDownloadResult.failure(StrUtil.maxLength(ex.getMessage(), 255), null);
        }
    }

    public GeweContactListResult getContactList(AgentWechatAccountDO account) {
        GeweEndpoint endpoint = resolveEndpoint(account);
        if (endpoint == null || StrUtil.isBlank(account.getGeweAppId())) {
            return new GeweContactListResult(List.of(), List.of(), List.of(), null);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("appId", account.getGeweAppId());
        Map<String, Object> response = tryPost(endpoint, "/contacts/fetchContactsList", body);
        Map<String, Object> cacheResponse = tryPost(endpoint, "/contacts/fetchContactsListCache", body);
        if (!isSuccess(response) && !isSuccess(cacheResponse)) {
            log.warn("[getContactList][accountId({}) failed: {}]", account.getId(),
                    response == null ? null : response.get("msg"));
            return new GeweContactListResult(List.of(), List.of(), List.of(), response);
        }
        GeweContactListResult live = isSuccess(response)
                ? toContactListResult(readDataMap(response), response)
                : new GeweContactListResult(List.of(), List.of(), List.of(), response);
        GeweContactListResult cached = isSuccess(cacheResponse)
                ? toContactListResult(readDataMap(cacheResponse), cacheResponse)
                : new GeweContactListResult(List.of(), List.of(), List.of(), cacheResponse);
        return live.merge(cached);
    }

    public Map<String, GeweContactInfo> getContactInfoMap(AgentWechatAccountDO account, List<String> wxids) {
        if (account == null || wxids == null || wxids.isEmpty()) {
            return Map.of();
        }
        GeweEndpoint endpoint = resolveEndpoint(account);
        if (endpoint == null || StrUtil.isBlank(account.getGeweAppId())) {
            return Map.of();
        }
        List<String> normalizedWxids = wxids.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        Map<String, GeweContactInfo> result = new HashMap<>();
        for (int start = 0; start < normalizedWxids.size(); start += 50) {
            List<String> batch = normalizedWxids.subList(start, Math.min(start + 50, normalizedWxids.size()));
            result.putAll(getBriefContactInfoMap(endpoint, account.getGeweAppId(), batch));
        }
        return result;
    }

    public GeweContactInfo getContactInfo(AgentWechatAccountDO account, String wxid) {
        if (account == null || StrUtil.isBlank(wxid)) {
            return null;
        }
        GeweEndpoint endpoint = resolveEndpoint(account);
        if (endpoint == null || StrUtil.isBlank(account.getGeweAppId())) {
            return null;
        }
        try {
            GeweContactInfo info = StrUtil.endWith(wxid, "@chatroom")
                    ? getChatroomInfo(endpoint, account.getGeweAppId(), wxid) : null;
            if (hasDisplayName(info)) {
                return info;
            }
            info = getBriefContactInfo(endpoint, account.getGeweAppId(), wxid);
            if (hasDisplayName(info)) {
                return info;
            }
            info = getDetailContactInfo(endpoint, account.getGeweAppId(), wxid);
            return hasDisplayName(info) ? info : null;
        } catch (Exception ex) {
            log.warn("[getContactInfo][accountId({}) wxid({}) failed]", account.getId(), wxid, ex);
            return null;
        }
    }

    public GeweContactInfo getChatroomMemberInfo(AgentWechatAccountDO account, String chatroomId, String memberWxid) {
        if (account == null || StrUtil.isBlank(chatroomId) || StrUtil.isBlank(memberWxid)) {
            return null;
        }
        GeweEndpoint endpoint = resolveEndpoint(account);
        if (endpoint == null || StrUtil.isBlank(account.getGeweAppId())) {
            return null;
        }
        try {
            GeweContactInfo info = getChatroomMemberInfo(endpoint, account.getGeweAppId(), chatroomId, memberWxid);
            if (hasDisplayName(info)) {
                return info;
            }
        } catch (Exception ex) {
            log.warn("[getChatroomMemberInfo][accountId({}) chatroomId({}) memberWxid({}) failed]",
                    account.getId(), chatroomId, memberWxid, ex);
        }
        return getContactInfo(account, memberWxid);
    }

    private GeweContactInfo getBriefContactInfo(GeweEndpoint endpoint, String appId, String wxid) {
        Map<String, Object> body = new HashMap<>();
        body.put("appId", appId);
        body.put("wxids", List.of(wxid));
        Map<String, Object> response = post(endpoint.baseUrl(), endpoint.token(), "/contacts/getBriefInfo", body);
        return toContactInfo(wxid, firstDataItem(response.get("data"), wxid));
    }

    private GeweContactListResult toContactListResult(Map<String, Object> data, Map<String, Object> rawResponse) {
        return new GeweContactListResult(
                readStringList(data, "friends", "friend", "friendList", "friendsList", "contactList", "contacts",
                        "wxids", "list", "items"),
                readStringList(data, "chatrooms", "chatroom", "chatroomList", "chatroomsList", "groups", "groupList",
                        "rooms", "roomList"),
                readStringList(data, "ghs", "ghList", "officialAccounts", "official_accounts", "officialAccountList"),
                rawResponse);
    }

    private Map<String, GeweContactInfo> getBriefContactInfoMap(GeweEndpoint endpoint, String appId, List<String> wxids) {
        Map<String, Object> body = new HashMap<>();
        body.put("appId", appId);
        body.put("wxids", wxids);
        try {
            Map<String, Object> response = post(endpoint.baseUrl(), endpoint.token(), "/contacts/getBriefInfo", body);
            return dataItems(response.get("data")).stream()
                    .map(item -> toContactInfo(readString(item, "userName", "wxid", "chatroomId"), item))
                    .filter(Objects::nonNull)
                    .filter(info -> StrUtil.isNotBlank(info.wxid()))
                    .collect(Collectors.toMap(GeweContactInfo::wxid, Function.identity(), (left, right) -> left));
        } catch (Exception ex) {
            log.warn("[getBriefContactInfoMap][appId({}) size({}) failed]", appId, wxids.size(), ex);
            return Map.of();
        }
    }

    private GeweContactInfo getDetailContactInfo(GeweEndpoint endpoint, String appId, String wxid) {
        Map<String, Object> body = new HashMap<>();
        body.put("appId", appId);
        body.put("wxids", List.of(wxid));
        Map<String, Object> response = post(endpoint.baseUrl(), endpoint.token(), "/contacts/getDetailInfo", body);
        return toContactInfo(wxid, firstDataItem(response.get("data"), wxid));
    }

    private GeweContactInfo getChatroomInfo(GeweEndpoint endpoint, String appId, String chatroomId) {
        Map<String, Object> body = new HashMap<>();
        body.put("appId", appId);
        body.put("chatroomId", chatroomId);
        Map<String, Object> response = post(endpoint.baseUrl(), endpoint.token(), "/group/getChatroomInfo", body);
        Object data = response.get("data");
        Map<String, Object> item = data instanceof Map<?, ?> ? castMap(data) : firstDataItem(data, chatroomId);
        return toContactInfo(chatroomId, item);
    }

    private GeweContactInfo getChatroomMemberInfo(GeweEndpoint endpoint, String appId, String chatroomId,
                                                  String memberWxid) {
        Map<String, Object> body = new HashMap<>();
        body.put("appId", appId);
        body.put("chatroomId", chatroomId);
        body.put("wxids", List.of(memberWxid));
        body.put("memberWxids", List.of(memberWxid));

        GeweContactInfo info = tryGetChatroomMemberInfo(endpoint, body, "/group/getChatroomMemberInfo", memberWxid);
        if (hasDisplayName(info)) {
            return info;
        }
        info = tryGetChatroomMemberInfo(endpoint, body, "/group/getChatroomMemberDetail", memberWxid);
        if (hasDisplayName(info)) {
            return info;
        }
        return tryGetChatroomMemberInfo(endpoint, body, "/group/getChatroomMemberBriefInfo", memberWxid);
    }

    private GeweContactInfo tryGetChatroomMemberInfo(GeweEndpoint endpoint, Map<String, Object> body, String path,
                                                     String memberWxid) {
        try {
            Map<String, Object> response = post(endpoint.baseUrl(), endpoint.token(), path, body);
            Object data = response.get("data");
            Map<String, Object> item = firstDataItem(data, memberWxid);
            return toContactInfo(memberWxid, item);
        } catch (Exception ex) {
            log.debug("[tryGetChatroomMemberInfo][path({}) failed]", path, ex);
            return null;
        }
    }

    private GeweEndpoint resolveEndpoint(AgentWechatAccountDO account) {
        if (account == null) {
            return null;
        }
        if (account.getGeweCredentialId() != null && credentialMapper != null) {
            AgentGeweCredentialDO credential = credentialMapper.selectById(account.getGeweCredentialId());
            if (credential != null
                    && credential.getStatus() != null
                    && credential.getStatus() == AgentConstants.STATUS_ENABLE
                    && StrUtil.isAllNotBlank(credential.getGeweApiBaseUrl(), credential.getGeweToken())) {
                return new GeweEndpoint(credential.getGeweApiBaseUrl(), credential.getGeweToken());
            }
        }
        if (StrUtil.isAllNotBlank(account.getGeweApiBaseUrl(), account.getGeweToken())) {
            return new GeweEndpoint(account.getGeweApiBaseUrl(), account.getGeweToken());
        }
        return null;
    }

    private Map<String, Object> post(AgentGeweCredentialDO credential, String path, Map<String, Object> body) {
        return post(credential.getGeweApiBaseUrl(), credential.getGeweToken(), path, body);
    }

    private Map<String, Object> post(String baseUrl, String token, String path, Map<String, Object> body) {
        Map<String, String> headers = new HashMap<>();
        headers.put(TOKEN_HEADER, token);
        headers.put("Content-Type", "application/json");
        try {
            String responseBody = HttpUtils.post(buildUrl(baseUrl, path), headers, JsonUtils.toJsonString(body));
            return JsonUtils.getObjectMapper().readValue(responseBody, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException(StrUtil.maxLength(ex.getMessage(), 255), ex);
        }
    }

    private String buildUrl(String baseUrl, String path) {
        String normalized = StrUtil.removeSuffix(baseUrl.trim(), "/");
        if (normalized.endsWith("/gewe/v2/api")) {
            return normalized + path;
        }
        return normalized + "/gewe/v2/api" + path;
    }

    private Object normalizeMessageId(String msgId) {
        String text = StrUtil.trim(msgId);
        if (StrUtil.isBlank(text) || !text.matches("-?\\d+")) {
            return text;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ex) {
            return text;
        }
    }

    private byte[] readLimited(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_MEDIA_BYTES) {
                throw new IOException("Media file is too large");
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private String resolveMediaContentType(String fileUrl, String upstreamContentType) {
        String contentType = StrUtil.blankToDefault(upstreamContentType, "").toLowerCase();
        if (StrUtil.isNotBlank(contentType) && !contentType.contains("octet-stream")) {
            return upstreamContentType;
        }
        String lowerUrl = StrUtil.blankToDefault(fileUrl, "").toLowerCase();
        if (lowerUrl.contains(".mp3")) {
            return "audio/mpeg";
        }
        if (lowerUrl.contains(".wav")) {
            return "audio/wav";
        }
        if (lowerUrl.contains(".amr")) {
            return "audio/amr";
        }
        if (lowerUrl.contains(".silk")) {
            return "audio/silk";
        }
        return StrUtil.blankToDefault(upstreamContentType, "application/octet-stream");
    }

    private boolean isSuccess(Map<String, Object> response) {
        Object ret = response.get("ret");
        if (ret instanceof Number number) {
            return number.intValue() == 200;
        }
        return StrUtil.equals(String.valueOf(ret), "200");
    }

    private Map<String, Object> tryPost(GeweEndpoint endpoint, String path, Map<String, Object> body) {
        try {
            return post(endpoint.baseUrl(), endpoint.token(), path, body);
        } catch (Exception ex) {
            log.warn("[tryPost][path({}) failed]", path, ex);
            return Map.of("ret", 500, "msg", StrUtil.maxLength(ex.getMessage(), 255));
        }
    }

    private boolean hasDisplayName(GeweContactInfo info) {
        return info != null
                && (hasHumanName(info.remark(), info.wxid()) || hasHumanName(info.nickname(), info.wxid()));
    }

    private boolean hasHumanName(String value, String wxid) {
        String text = GeweScalarNormalizer.cleanText(value);
        return StrUtil.isNotBlank(text)
                && !StrUtil.equals(text, wxid)
                && !GeweScalarNormalizer.isRawWechatIdentifier(text);
    }

    private GeweContactInfo toContactInfo(String wxid, Map<String, Object> item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        String resolvedWxid = StrUtil.blankToDefault(readString(item, "userName", "wxid", "chatroomId",
                "memberWxid", "memberUserName", "actualUserName"), wxid);
        String nickname = readString(item, "nickName", "nickname", "displayName", "chatroomName", "roomName",
                "groupName", "memberNickName", "memberNickname", "actualNickName", "actualNickname", "name");
        String remark = readString(item, "remark", "remarkName", "conRemark", "displayRemark");
        String avatar = StrUtil.blankToDefault(readString(item, "bigHeadImgUrl", "bigHeadUrl", "avatar"),
                readString(item, "smallHeadImgUrl", "smallHeadUrl", "headImgUrl"));
        return new GeweContactInfo(resolvedWxid, clean(nickname), clean(remark), clean(avatar));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> dataItems(Object data) {
        if (data instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        if (data instanceof Map<?, ?> map) {
            Object list = map.get("list");
            if (list instanceof List<?>) {
                return dataItems(list);
            }
            Object items = map.get("items");
            if (items instanceof List<?>) {
                return dataItems(items);
            }
            Object dataList = map.get("data");
            if (dataList instanceof List<?>) {
                return dataItems(dataList);
            }
            return List.of((Map<String, Object>) map);
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstDataItem(Object data, String wxid) {
        if (data instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> itemMap) {
                    Map<String, Object> typed = (Map<String, Object>) itemMap;
                    String itemWxid = readString(typed, "userName", "wxid", "chatroomId",
                            "memberWxid", "memberUserName", "actualUserName");
                    if (StrUtil.isBlank(itemWxid) || StrUtil.equals(itemWxid, wxid)) {
                        return typed;
                    }
                }
            }
        }
        if (data instanceof Map<?, ?> map) {
            Object list = map.get("list");
            if (list instanceof List<?>) {
                return firstDataItem(list, wxid);
            }
            Object members = map.get("members");
            if (members instanceof List<?>) {
                return firstDataItem(members, wxid);
            }
            Object memberList = map.get("memberList");
            if (memberList instanceof List<?>) {
                return firstDataItem(memberList, wxid);
            }
            return (Map<String, Object>) map;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object data) {
        return (Map<String, Object>) data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readDataMap(Map<String, Object> response) {
        if (!isSuccess(response)) {
            throw new IllegalStateException(String.valueOf(response.getOrDefault("msg", "Gewe 请求失败")));
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            return (Map<String, Object>) dataMap;
        }
        return Map.of();
    }

    private String readString(Map<String, Object> data, String field) {
        Object value = data.get(field);
        return value == null ? null : GeweScalarNormalizer.toWechatString(value);
    }

    private String readString(Map<String, Object> data, String... fields) {
        for (String field : fields) {
            String value = readString(data, field);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String firstString(Map<String, Object> primary, Map<String, Object> secondary, String... fields) {
        String value = readString(primary, fields);
        return StrUtil.isNotBlank(value) ? value : readString(secondary, fields);
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(Map<String, Object> data, String... fields) {
        for (String field : fields) {
            Object value = data.get(field);
            if (value instanceof List<?> list) {
                return list.stream()
                        .map(item -> item instanceof Map<?, ?> map
                                ? readString((Map<String, Object>) map, "userName", "wxid", "chatroomId", "id")
                                : GeweScalarNormalizer.toWechatString(item))
                        .filter(StrUtil::isNotBlank)
                        .distinct()
                        .toList();
            }
            if (value instanceof Map<?, ?> map) {
                List<String> nested = readStringList((Map<String, Object>) map, "list", "items", "data");
                if (!nested.isEmpty()) {
                    return nested;
                }
            }
            String scalar = value == null ? null : GeweScalarNormalizer.toWechatString(value);
            if (StrUtil.isNotBlank(scalar)) {
                return List.of(scalar);
            }
        }
        return List.of();
    }

    private String clean(String value) {
        String text = GeweScalarNormalizer.cleanText(value);
        return StrUtil.isBlank(text) ? null : text;
    }

    private Integer firstInt(Map<String, Object> primary, Map<String, Object> secondary, String... fields) {
        Integer value = readInt(primary, fields);
        return value != null ? value : readInt(secondary, fields);
    }

    private Integer readInt(Map<String, Object> data, String... fields) {
        if (data == null) {
            return null;
        }
        for (String field : fields) {
            Integer value = readInt(data, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Integer readInt(Map<String, Object> data, String field) {
        if (data == null) {
            return null;
        }
        Object value = data.get(field);
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = value == null ? null : GeweScalarNormalizer.toWechatString(value);
        if (StrUtil.isBlank(text) || !text.trim().matches("-?\\d+")) {
            return null;
        }
        return Integer.valueOf(text.trim());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(Map<String, Object> data, String... fields) {
        if (data == null) {
            return Map.of();
        }
        for (String field : fields) {
            Object value = GeweScalarNormalizer.unwrap(data.get(field));
            if (value instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            if (value instanceof String text) {
                Map<String, Object> parsed = JsonUtils.parseObjectQuietly(text, new TypeReference<>() {
                });
                if (parsed != null && !parsed.isEmpty()) {
                    return parsed;
                }
            }
        }
        return Map.of();
    }

    private boolean isLoginSuccess(Map<String, Object> data, Map<String, Object> loginInfo,
                                   Integer status, String statusText) {
        if (status != null && status >= 2) {
            return true;
        }
        String normalizedStatus = StrUtil.blankToDefault(statusText, "").trim().toLowerCase();
        if (StrUtil.equalsAny(normalizedStatus, "success", "succeed", "login_success", "logged_in", "online",
                "confirmed", "bound", "ok", "true")
                || StrUtil.containsAny(normalizedStatus, "登录成功", "已登录", "确认成功", "授权成功")) {
            return true;
        }
        return hasWechatIdentity(data) || hasWechatIdentity(loginInfo);
    }

    private boolean hasWechatIdentity(Map<String, Object> data) {
        return data != null && (StrUtil.isNotBlank(readString(data, "wxid", "userName", "username"))
                || StrUtil.isNotBlank(readString(data, "uin"))
                || StrUtil.isNotBlank(readString(data, "sessionKey", "session_key")));
    }

    private boolean isWaitingConfirm(Integer status, String statusText, String verifyUrl,
                                     String nickName, String avatar) {
        if (status != null && status == 1) {
            return true;
        }
        String normalizedStatus = StrUtil.blankToDefault(statusText, "").trim().toLowerCase();
        return StrUtil.containsAny(normalizedStatus, "scan", "scanned", "confirm", "wait_confirm",
                "扫码", "已扫码", "确认", "待确认", "等待确认")
                || StrUtil.isNotBlank(verifyUrl)
                || StrUtil.isNotBlank(nickName)
                || StrUtil.isNotBlank(avatar);
    }

    private String extractUuidFromQrData(String qrData) {
        if (StrUtil.isBlank(qrData)) {
            return null;
        }
        return StrUtil.subAfter(qrData, "/", true);
    }

    @SuppressWarnings("unchecked")
    private String readNewMsgId(Map<String, Object> response) {
        Object data = response.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            return null;
        }
        Object newMsgId = ((Map<String, Object>) dataMap).get("newMsgId");
        return newMsgId == null ? null : String.valueOf(newMsgId);
    }

    private record GeweEndpoint(String baseUrl, String token) {
    }

    public record DownloadedMedia(byte[] body, String contentType) {
    }

}
