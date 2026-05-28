package cn.ai.sales.module.agent.service.gewe;

import cn.ai.sales.framework.common.util.http.HttpUtils;
import cn.ai.sales.module.agent.dal.dataobject.AgentGeweCredentialDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class GeweMessageClientTest {

    private final GeweMessageClient client = new GeweMessageClient();

    @Test
    void getContactListMergesLiveAndCachedLists() {
        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setId(1L);
        account.setGeweAppId("app-1");
        account.setGeweApiBaseUrl("http://gewe.local");
        account.setGeweToken("token-1");

        String liveResponse = """
                {
                  "ret": 200,
                  "msg": "ok",
                  "data": {
                    "friends": ["wxid_a", "wxid_b"],
                    "chatrooms": ["room_a@chatroom"]
                  }
                }
                """;
        String cacheResponse = """
                {
                  "ret": 200,
                  "msg": "ok",
                  "data": {
                    "friendList": ["wxid_b", "wxid_c"],
                    "chatroomList": ["room_b@chatroom"]
                  }
                }
                """;

        try (MockedStatic<HttpUtils> http = mockStatic(HttpUtils.class)) {
            http.when(() -> HttpUtils.post(eq("http://gewe.local/gewe/v2/api/contacts/fetchContactsList"),
                            argThat(headers -> "token-1".equals(headers.get("X-GEWE-TOKEN"))),
                            argThat(body -> body.contains("\"appId\":\"app-1\""))))
                    .thenReturn(liveResponse);
            http.when(() -> HttpUtils.post(eq("http://gewe.local/gewe/v2/api/contacts/fetchContactsListCache"),
                            argThat(headers -> "token-1".equals(headers.get("X-GEWE-TOKEN"))),
                            argThat(body -> body.contains("\"appId\":\"app-1\""))))
                    .thenReturn(cacheResponse);

            GeweContactListResult result = client.getContactList(account);

            assertThat(result.friends()).containsExactly("wxid_a", "wxid_b", "wxid_c");
            assertThat(result.chatrooms()).containsExactly("room_a@chatroom", "room_b@chatroom");
            assertThat(result.syncableContactIds()).containsExactly("wxid_a", "wxid_b", "wxid_c",
                    "room_a@chatroom", "room_b@chatroom");
        }
    }

    @Test
    void getContactInfoUsesBriefInfoAndParsesNickName() {
        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setId(1L);
        account.setGeweAppId("app-1");
        account.setGeweApiBaseUrl("http://gewe.local");
        account.setGeweToken("token-1");

        String response = """
                {
                  "ret": 200,
                  "msg": "ok",
                  "data": [
                    {
                      "userName": "wxid_customer",
                      "nickName": "Rain Zhang",
                      "remark": "",
                      "bigHeadImgUrl": "https://wx.qlogo.cn/avatar/0",
                      "smallHeadImgUrl": "https://wx.qlogo.cn/avatar/132"
                    }
                  ]
                }
                """;

        try (MockedStatic<HttpUtils> http = mockStatic(HttpUtils.class)) {
            http.when(() -> HttpUtils.post(eq("http://gewe.local/gewe/v2/api/contacts/getBriefInfo"),
                            argThat(headers -> "token-1".equals(headers.get("X-GEWE-TOKEN"))),
                            argThat(body -> body.contains("\"appId\":\"app-1\"")
                                    && body.contains("\"wxids\":[\"wxid_customer\"]"))))
                    .thenReturn(response);

            GeweContactInfo info = client.getContactInfo(account, "wxid_customer");

            assertThat(info).isNotNull();
            assertThat(info.wxid()).isEqualTo("wxid_customer");
            assertThat(info.nickname()).isEqualTo("Rain Zhang");
            assertThat(info.avatar()).isEqualTo("https://wx.qlogo.cn/avatar/0");
            http.verify(() -> HttpUtils.post(eq("http://gewe.local/gewe/v2/api/contacts/getBriefInfo"),
                    argThat(headers -> "token-1".equals(headers.get("X-GEWE-TOKEN"))),
                    argThat(body -> body.contains("\"wxids\":[\"wxid_customer\"]"))));
        }
    }

    @Test
    void downloadVoiceCallsGeweDownloadVoiceApi() {
        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setId(1L);
        account.setGeweAppId("app-1");
        account.setGeweApiBaseUrl("http://gewe.local");
        account.setGeweToken("token-1");

        String response = """
                {
                  "ret": 200,
                  "msg": "ok",
                  "data": {
                    "fileUrl": "https://voice.example.com/voice.silk"
                  }
                }
                """;

        try (MockedStatic<HttpUtils> http = mockStatic(HttpUtils.class)) {
            http.when(() -> HttpUtils.post(eq("http://gewe.local/gewe/v2/api/message/downloadVoice"),
                            argThat(headers -> "token-1".equals(headers.get("X-GEWE-TOKEN"))),
                            argThat(body -> body.contains("\"appId\":\"app-1\"")
                                    && body.contains("\"msgId\":123")
                                    && body.contains("<voicemsg"))))
                    .thenReturn(response);

            GeweVoiceDownloadResult result = client.downloadVoice(account, "123",
                    "<msg><voicemsg voicelength=\"1200\" /></msg>");

            assertThat(result.success()).isTrue();
            assertThat(result.fileUrl()).isEqualTo("https://voice.example.com/voice.silk");
        }
    }

    @Test
    void downloadImageCallsGeweDownloadImageApi() {
        AgentWechatAccountDO account = account("app-1", "http://gewe.local", "token-1");
        String response = """
                {"ret":200,"msg":"ok","data":{"fileUrl":"https://media.example.com/a.png"}}
                """;

        try (MockedStatic<HttpUtils> http = mockStatic(HttpUtils.class)) {
            http.when(() -> HttpUtils.post(eq("http://gewe.local/gewe/v2/api/message/downloadImage"),
                            argThat(headers -> "token-1".equals(headers.get("X-GEWE-TOKEN"))),
                            argThat(body -> body.contains("\"appId\":\"app-1\"")
                                    && body.contains("\"type\":2")
                                    && body.contains("<img"))))
                    .thenReturn(response);

            GeweMediaDownloadResult result = client.downloadImage(account, "<msg><img /></msg>");

            assertThat(result.success()).isTrue();
            assertThat(result.fileUrl()).isEqualTo("https://media.example.com/a.png");
        }
    }

    @Test
    void downloadEmojiCallsGeweDownloadEmojiApi() {
        AgentWechatAccountDO account = account("app-1", "http://gewe.local", "token-1");
        String response = """
                {"ret":200,"msg":"ok","data":{"url":"https://media.example.com/e.gif"}}
                """;

        try (MockedStatic<HttpUtils> http = mockStatic(HttpUtils.class)) {
            http.when(() -> HttpUtils.post(eq("http://gewe.local/gewe/v2/api/message/downloadEmojiMd5"),
                            argThat(headers -> "token-1".equals(headers.get("X-GEWE-TOKEN"))),
                            argThat(body -> body.contains("\"appId\":\"app-1\"")
                                    && body.contains("\"emojiMd5\":\"abc123\""))))
                    .thenReturn(response);

            GeweMediaDownloadResult result = client.downloadEmoji(account, "abc123");

            assertThat(result.success()).isTrue();
            assertThat(result.fileUrl()).isEqualTo("https://media.example.com/e.gif");
        }
    }

    @Test
    void downloadFileCallsGeweDownloadFileApi() {
        AgentWechatAccountDO account = account("app-1", "http://gewe.local", "token-1");
        String response = """
                {"ret":200,"msg":"ok","data":{"fileUrl":"https://media.example.com/a.pdf"}}
                """;

        try (MockedStatic<HttpUtils> http = mockStatic(HttpUtils.class)) {
            http.when(() -> HttpUtils.post(eq("http://gewe.local/gewe/v2/api/message/downloadFile"),
                            argThat(headers -> "token-1".equals(headers.get("X-GEWE-TOKEN"))),
                            argThat(body -> body.contains("\"appId\":\"app-1\"")
                                    && body.contains("<appmsg"))))
                    .thenReturn(response);

            GeweMediaDownloadResult result = client.downloadFile(account, "<msg><appmsg /></msg>");

            assertThat(result.success()).isTrue();
            assertThat(result.fileUrl()).isEqualTo("https://media.example.com/a.pdf");
        }
    }

    @Test
    void checkLoginTreatsScannedStatusAsWaitingConfirm() {
        AgentGeweCredentialDO credential = new AgentGeweCredentialDO();
        credential.setGeweApiBaseUrl("http://gewe.local");
        credential.setGeweToken("token-1");

        String response = """
                {
                  "ret": 200,
                  "msg": "ok",
                  "data": {
                    "uuid": "uuid-1",
                    "headImgUrl": "http://avatar",
                    "nickName": "G",
                    "status": 1,
                    "loginInfo": null
                  }
                }
                """;

        try (MockedStatic<HttpUtils> http = mockStatic(HttpUtils.class)) {
            http.when(() -> HttpUtils.post(eq("http://gewe.local/gewe/v2/api/login/checkLogin"),
                            argThat(headers -> "token-1".equals(headers.get("X-GEWE-TOKEN"))),
                            argThat(body -> body.contains("\"appId\":\"app-1\"")
                                    && body.contains("\"uuid\":\"uuid-1\""))))
                    .thenReturn(response);

            GeweLoginCheckResult result = client.checkLogin(credential, "app-1", "uuid-1");

            assertThat(result.success()).isFalse();
            assertThat(result.waitConfirm()).isTrue();
            assertThat(result.nickName()).isEqualTo("G");
            assertThat(result.avatar()).isEqualTo("http://avatar");
        }
    }

    @Test
    void checkLoginTreatsNestedLoginInfoAsSuccess() {
        AgentGeweCredentialDO credential = new AgentGeweCredentialDO();
        credential.setGeweApiBaseUrl("http://gewe.local");
        credential.setGeweToken("token-1");

        String response = """
                {
                  "ret": 200,
                  "msg": "ok",
                  "data": {
                    "loginInfo": {
                      "wxid": "wxid_owner",
                      "nickName": "Owner",
                      "bigHeadImgUrl": "http://avatar"
                    },
                    "loginStatus": "2"
                  }
                }
                """;

        try (MockedStatic<HttpUtils> http = mockStatic(HttpUtils.class)) {
            http.when(() -> HttpUtils.post(eq("http://gewe.local/gewe/v2/api/login/checkLogin"),
                            argThat(headers -> "token-1".equals(headers.get("X-GEWE-TOKEN"))),
                            argThat(body -> body.contains("\"appId\":\"app-1\"")
                                    && body.contains("\"uuid\":\"uuid-1\""))))
                    .thenReturn(response);

            GeweLoginCheckResult result = client.checkLogin(credential, "app-1", "uuid-1");

            assertThat(result.success()).isTrue();
            assertThat(result.waitConfirm()).isFalse();
            assertThat(result.nickName()).isEqualTo("Owner");
            assertThat(result.avatar()).isEqualTo("http://avatar");
        }
    }

    @Test
    void checkLoginTreatsAlternateConfirmFieldsAsWaitingConfirm() {
        AgentGeweCredentialDO credential = new AgentGeweCredentialDO();
        credential.setGeweApiBaseUrl("http://gewe.local");
        credential.setGeweToken("token-1");

        String response = """
                {
                  "ret": 200,
                  "msg": "ok",
                  "data": {
                    "state": "wait_confirm",
                    "confirmUrl": "https://verify.example/confirm",
                    "nickname": "Owner",
                    "avatar": "http://avatar"
                  }
                }
                """;

        try (MockedStatic<HttpUtils> http = mockStatic(HttpUtils.class)) {
            http.when(() -> HttpUtils.post(eq("http://gewe.local/gewe/v2/api/login/checkLogin"),
                            argThat(headers -> "token-1".equals(headers.get("X-GEWE-TOKEN"))),
                            argThat(body -> body.contains("\"appId\":\"app-1\"")
                                    && body.contains("\"uuid\":\"uuid-1\""))))
                    .thenReturn(response);

            GeweLoginCheckResult result = client.checkLogin(credential, "app-1", "uuid-1");

            assertThat(result.success()).isFalse();
            assertThat(result.waitConfirm()).isTrue();
            assertThat(result.verifyUrl()).isEqualTo("https://verify.example/confirm");
            assertThat(result.nickName()).isEqualTo("Owner");
            assertThat(result.avatar()).isEqualTo("http://avatar");
        }
    }

    private AgentWechatAccountDO account(String appId, String baseUrl, String token) {
        AgentWechatAccountDO account = new AgentWechatAccountDO();
        account.setId(1L);
        account.setGeweAppId(appId);
        account.setGeweApiBaseUrl(baseUrl);
        account.setGeweToken(token);
        return account;
    }

}
