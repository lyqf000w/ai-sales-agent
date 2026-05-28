package cn.ai.sales.module.agent.service.conversation;

import cn.hutool.core.util.StrUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

@Service
public class AgentWechatMediaProxyService {

    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 15_000;
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final String[] ALLOWED_HOST_SUFFIXES = {
            "qq.com", "qpic.cn", "qlogo.cn", "weixin.qq.com", "wechat.com",
            "test.sales.iocoder.cn", "static.sales.iocoder.cn", "mall.sales.iocoder.cn"
    };

    public ProxiedMedia fetch(String url, String aesKey) throws IOException {
        return fetchAllowedMedia(URI.create(url), 0, aesKey);
    }

    private ProxiedMedia fetchAllowedMedia(URI uri, int redirectCount, String aesKey) throws IOException {
        if (!isAllowedMediaProxyUri(uri)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported media url");
        }
        if (redirectCount > MAX_REDIRECTS) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Too many media redirects");
        }
        try {
            return fetchMedia(uri, redirectCount, aesKey);
        } catch (SSLException ex) {
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                return fetchMedia(replaceScheme(uri, "http"), redirectCount, aesKey);
            }
            throw ex;
        }
    }

    private ProxiedMedia fetchMedia(URI uri, int redirectCount, String aesKey) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty(HttpHeaders.USER_AGENT, "Mozilla/5.0");
        connection.setRequestProperty(HttpHeaders.ACCEPT,
                "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        int status = connection.getResponseCode();
        if (isRedirect(status)) {
            String location = connection.getHeaderField(HttpHeaders.LOCATION);
            if (location == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid media redirect");
            }
            return fetchAllowedMedia(uri.resolve(location), redirectCount + 1, aesKey);
        }
        if (status < 200 || status >= 300) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch media");
        }
        try (InputStream inputStream = connection.getInputStream()) {
            byte[] body = readLimited(inputStream);
            byte[] normalized = decryptWechatMediaIfNeeded(body, aesKey);
            return new ProxiedMedia(normalized, resolveMediaContentType(normalized, connection.getContentType()));
        } finally {
            connection.disconnect();
        }
    }

    byte[] decryptWechatMediaIfNeeded(byte[] body, String aesKey) throws IOException {
        if (body.length == 0 || StrUtil.isBlank(aesKey) || isKnownImage(body)) {
            return body;
        }
        byte[] key = decodeWechatAesKey(aesKey);
        if (key == null || body.length % 16 != 0) {
            return body;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new IvParameterSpec(Arrays.copyOf(key, 16)));
            byte[] decrypted = stripPkcsPadding(cipher.doFinal(body));
            return isKnownImage(decrypted) ? decrypted : body;
        } catch (GeneralSecurityException ex) {
            throw new IOException("Failed to decrypt WeChat media", ex);
        }
    }

    String resolveMediaContentType(byte[] body, String upstreamContentType) {
        String sniffed = sniffImageContentType(body);
        if (sniffed != null) {
            return sniffed;
        }
        return StrUtil.blankToDefault(upstreamContentType, "application/octet-stream");
    }

    private byte[] decodeWechatAesKey(String aesKey) {
        String normalized = AgentWechatDisplayFormatter.cleanScalar(aesKey);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        if (isHexAesKey(normalized)) {
            return decodeHexAesKey(normalized);
        }
        int padding = (4 - normalized.length() % 4) % 4;
        normalized = normalized + "=".repeat(padding);
        try {
            byte[] key = Base64.getDecoder().decode(normalized);
            return key.length == 16 || key.length == 24 || key.length == 32 ? key : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isHexAesKey(String aesKey) {
        int length = aesKey.length();
        if (length != 32 && length != 48 && length != 64) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char ch = aesKey.charAt(i);
            if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private byte[] decodeHexAesKey(String aesKey) {
        byte[] key = new byte[aesKey.length() / 2];
        for (int i = 0; i < key.length; i++) {
            int high = Character.digit(aesKey.charAt(i * 2), 16);
            int low = Character.digit(aesKey.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                return null;
            }
            key[i] = (byte) ((high << 4) + low);
        }
        return key;
    }

    private byte[] stripPkcsPadding(byte[] body) {
        if (body.length == 0) {
            return body;
        }
        int padding = body[body.length - 1] & 0xff;
        if (padding <= 0 || padding > 16 || padding > body.length) {
            return body;
        }
        for (int i = body.length - padding; i < body.length; i++) {
            if ((body[i] & 0xff) != padding) {
                return body;
            }
        }
        return Arrays.copyOf(body, body.length - padding);
    }

    private String sniffImageContentType(byte[] body) {
        if (body.length >= 8
                && (body[0] & 0xff) == 0x89 && body[1] == 0x50 && body[2] == 0x4e && body[3] == 0x47
                && body[4] == 0x0d && body[5] == 0x0a && body[6] == 0x1a && body[7] == 0x0a) {
            return "image/png";
        }
        if (body.length >= 3 && (body[0] & 0xff) == 0xff && (body[1] & 0xff) == 0xd8
                && (body[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (body.length >= 6 && body[0] == 0x47 && body[1] == 0x49 && body[2] == 0x46
                && body[3] == 0x38 && (body[4] == 0x37 || body[4] == 0x39) && body[5] == 0x61) {
            return "image/gif";
        }
        if (body.length >= 12 && body[0] == 0x52 && body[1] == 0x49 && body[2] == 0x46 && body[3] == 0x46
                && body[8] == 0x57 && body[9] == 0x45 && body[10] == 0x42 && body[11] == 0x50) {
            return "image/webp";
        }
        return null;
    }

    private boolean isKnownImage(byte[] body) {
        return sniffImageContentType(body) != null;
    }

    private URI replaceScheme(URI uri, String scheme) {
        return URI.create(scheme + ":" + uri.getRawSchemeSpecificPart() + (uri.getRawFragment() == null
                ? ""
                : "#" + uri.getRawFragment()));
    }

    boolean isAllowedMediaProxyUri(URI uri) {
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return false;
        }
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        for (String suffix : ALLOWED_HOST_SUFFIXES) {
            if (normalizedHost.equals(suffix) || normalizedHost.endsWith("." + suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_SEE_OTHER
                || status == 307
                || status == 308;
    }

    private byte[] readLimited(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_BYTES) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Media file is too large");
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    public record ProxiedMedia(byte[] body, String contentType) {
    }

}
