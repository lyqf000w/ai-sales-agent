package cn.ai.sales.module.agent.service.conversation;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AgentWechatMediaProxyServiceTest {

    private final AgentWechatMediaProxyService service = new AgentWechatMediaProxyService();

    @Test
    void decryptWechatMediaIfNeededRestoresEncryptedImageBytes() throws Exception {
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes();
        byte[] image = new byte[] {
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
                0x01, 0x02, 0x03, 0x04
        };
        byte[] encrypted = encryptWechatStyle(image, key);
        String aesKey = Base64.getEncoder().withoutPadding().encodeToString(key);

        byte[] decrypted = service.decryptWechatMediaIfNeeded(encrypted, aesKey);

        assertThat(decrypted).isEqualTo(image);
        assertThat(service.resolveMediaContentType(decrypted, "application/octet-stream"))
                .isEqualTo("image/png");
    }

    @Test
    void decryptWechatMediaIfNeededSupportsHexAesKeyFromWechatCdn() throws Exception {
        byte[] key = fromHex("cb7a5c9fa0cf43579692b2abe9047041");
        byte[] image = new byte[] {
                (byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0,
                0x00, 0x10, 0x4a, 0x46, 0x49, 0x46
        };
        byte[] encrypted = encryptWechatStyle(image, key);

        byte[] decrypted = service.decryptWechatMediaIfNeeded(encrypted,
                "cb7a5c9fa0cf43579692b2abe9047041");

        assertThat(decrypted).isEqualTo(image);
        assertThat(service.resolveMediaContentType(decrypted, "application/octet-stream"))
                .isEqualTo("image/jpeg");
    }

    @Test
    void isAllowedMediaProxyUriAllowsLegacyFileHosts() {
        assertThat(service.isAllowedMediaProxyUri(URI.create(
                "http://test.sales.iocoder.cn/user/avatar/20251220/blob.jpg"))).isTrue();
        assertThat(service.isAllowedMediaProxyUri(URI.create(
                "https://static.sales.iocoder.cn/static/demo.png"))).isTrue();
        assertThat(service.isAllowedMediaProxyUri(URI.create(
                "http://snsvideo.hk.wechat.com/262/20304/stdownload?m=abc"))).isTrue();
        assertThat(service.isAllowedMediaProxyUri(URI.create(
                "https://example.com/static/demo.png"))).isFalse();
    }

    private byte[] encryptWechatStyle(byte[] body, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new IvParameterSpec(Arrays.copyOf(key, 16)));
        return cipher.doFinal(padPkcs(body));
    }

    private byte[] padPkcs(byte[] body) {
        int padding = 16 - body.length % 16;
        byte[] padded = Arrays.copyOf(body, body.length + padding);
        Arrays.fill(padded, body.length, padded.length, (byte) padding);
        return padded;
    }

    private byte[] fromHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

}
