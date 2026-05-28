package cn.ai.sales.module.agent.controller.admin.conversation;

import cn.ai.sales.framework.common.pojo.CommonResult;
import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactPageReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationContactRespVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationContactSettingsRespVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationContactSettingsSaveReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationMessageReviewReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationPageReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationRestoreOriginalPolicyReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationRespVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationSendMessageReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentMessageRespVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.conversation.AgentConversationService;
import cn.ai.sales.module.agent.service.conversation.AgentWechatMediaProxyService;
import cn.ai.sales.module.agent.service.conversation.AgentWechatDisplayFormatter;
import cn.ai.sales.framework.tenant.core.aop.TenantIgnore;
import cn.ai.sales.module.agent.service.gewe.GeweMessageClient;
import cn.ai.sales.module.agent.service.gewe.GeweMediaDownloadResult;
import cn.ai.sales.module.agent.service.gewe.GeweVoiceDownloadResult;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.framework.common.pojo.CommonResult.success;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.CONVERSATION_NOT_EXISTS;

@Tag(name = "管理后台 - AI 销冠会话")
@RestController
@RequestMapping("/agent/conversation")
@Validated
public class AgentConversationController {

    @Resource
    private AgentConversationService conversationService;
    @Resource
    private AgentWechatMediaProxyService mediaProxyService;
    @Resource
    private AgentWechatAccountMapper accountMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private AgentConversationMapper conversationMapper;
    @Resource
    private AgentMessageMapper messageMapper;
    @Resource
    private GeweMessageClient geweMessageClient;

    @GetMapping("/page")
    @Operation(summary = "获得会话分页")
    @PreAuthorize("@ss.hasPermission('agent:conversation:query')")
    public CommonResult<PageResult<AgentConversationRespVO>> getConversationPage(
            @Valid AgentConversationPageReqVO pageReqVO) {
        PageResult<AgentConversationDO> pageResult = conversationService.getConversationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AgentConversationRespVO.class));
    }

    @GetMapping("/contact-page")
    @Operation(summary = "获得会话好友队列分页")
    @PreAuthorize("@ss.hasPermission('agent:conversation:query')")
    public CommonResult<PageResult<AgentConversationContactRespVO>> getConversationContactPage(
            @Valid AgentWechatContactPageReqVO pageReqVO) {
        return success(conversationService.getConversationContactPage(pageReqVO));
    }

    @GetMapping("/messages")
    @Operation(summary = "获得会话消息列表")
    @Parameter(name = "conversationId", description = "会话编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:conversation:query')")
    public CommonResult<List<AgentMessageRespVO>> getConversationMessages(
            @RequestParam("conversationId") Long conversationId) {
        List<AgentMessageDO> messages = conversationService.getConversationMessages(conversationId);
        return success(messages.stream().map(this::messageResp).toList());
    }

    @GetMapping("/media-proxy")
    @Operation(summary = "Proxy WeChat media")
    @PermitAll
    @TenantIgnore
    public ResponseEntity<byte[]> proxyWechatMedia(@RequestParam("url") String url,
                                                   @RequestParam(value = "aesKey", required = false) String aesKey) {
        try {
            AgentWechatMediaProxyService.ProxiedMedia media = mediaProxyService.fetch(url, aesKey);
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.maxAge(Duration.ofHours(12)).cachePublic());
            headers.set(HttpHeaders.CONTENT_TYPE, media.contentType());
            return ResponseEntity.ok().headers(headers).body(media.body());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media url", ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch media", ex);
        }
    }

    @GetMapping("/voice-media")
    @Operation(summary = "Download WeChat voice through Gewe")
    @Parameter(name = "messageId", description = "Message id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:conversation:query')")
    public ResponseEntity<byte[]> downloadVoiceMedia(@RequestParam("messageId") Long messageId) {
        AgentMessageDO message = messageMapper.selectById(messageId);
        if (message == null || !Objects.equals(message.getMessageType(), AgentConstants.MESSAGE_TYPE_VOICE)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voice message not found");
        }
        AgentWechatAccountDO account = accountMapper.selectById(message.getWechatAccountId());
        String voiceXml = AgentWechatDisplayFormatter.extractVoiceXml(message.getContent(), message.getRawPayload());
        GeweVoiceDownloadResult result = geweMessageClient.downloadVoice(account, message.getGeweMessageId(), voiceXml);
        if (!result.success()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    StrUtil.blankToDefault(result.errorMessage(), "Failed to download voice"));
        }
        try {
            GeweMessageClient.DownloadedMedia media = geweMessageClient.downloadMediaFile(result.fileUrl());
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noCache());
            headers.set(HttpHeaders.CONTENT_TYPE, media.contentType());
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"wechat-voice-" + messageId + mediaFileExtension(media.contentType()) + "\"");
            return ResponseEntity.ok().headers(headers).body(media.body());
        } catch (IOException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch voice media", ex);
        }
    }

    @GetMapping("/gewe-media")
    @Operation(summary = "Download WeChat media through Gewe")
    @Parameter(name = "messageId", description = "Message id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:conversation:query')")
    public ResponseEntity<byte[]> downloadGeweMedia(@RequestParam("messageId") Long messageId) {
        AgentMessageDO message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found");
        }
        AgentWechatAccountDO account = accountMapper.selectById(message.getWechatAccountId());
        GeweMediaDownloadResult result = downloadByMessageType(account, message);
        if (!result.success()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    StrUtil.blankToDefault(result.errorMessage(), "Failed to download media"));
        }
        try {
            GeweMessageClient.DownloadedMedia media = geweMessageClient.downloadMediaFile(result.fileUrl());
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noCache());
            headers.set(HttpHeaders.CONTENT_TYPE, media.contentType());
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    contentDisposition(message, media.contentType(), messageId));
            return ResponseEntity.ok().headers(headers).body(media.body());
        } catch (IOException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch media file", ex);
        }
    }

    @GetMapping("/detail")
    @Operation(summary = "Get conversation detail")
    @Parameter(name = "conversationId", description = "Conversation id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('agent:conversation:query')")
    public CommonResult<Map<String, Object>> getConversationDetail(@RequestParam("conversationId") Long conversationId) {
        AgentConversationDO conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw exception(CONVERSATION_NOT_EXISTS);
        }
        AgentWechatAccountDO account = conversation.getWechatAccountId() == null
                ? null : accountMapper.selectById(conversation.getWechatAccountId());
        AgentWechatContactDO contact = conversation.getContactId() == null
                ? null : contactMapper.selectById(conversation.getContactId());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("conversation", BeanUtils.toBean(conversation, AgentConversationRespVO.class));
        detail.put("account", accountBrief(account));
        detail.put("contact", contactBrief(contact));
        detail.put("messages", conversationService.getConversationMessages(conversationId).stream()
                .map(this::messageResp).toList());
        return success(detail);
    }

    private AgentMessageRespVO messageResp(AgentMessageDO message) {
        AgentMessageRespVO respVO = BeanUtils.toBean(message, AgentMessageRespVO.class);
        boolean groupMessage = AgentWechatDisplayFormatter.isGroupMessage(message.getContent(), message.getRawPayload());
        String senderDisplayName = groupMessage
                ? AgentWechatDisplayFormatter.resolveSenderDisplayName(message.getContent(), message.getRawPayload(), null)
                : null;
        String displayContent = groupMessage
                ? AgentWechatDisplayFormatter.stripGroupSenderPrefix(message.getContent(), senderDisplayName)
                : message.getContent();
        respVO.setSenderDisplayName(senderDisplayName);
        respVO.setContent(AgentWechatDisplayFormatter.formatMessageContent(message.getMessageType(), displayContent));
        respVO.setMediaUrl(AgentWechatDisplayFormatter.extractMediaUrl(message.getMessageType(), message.getContent(),
                message.getRawPayload()));
        respVO.setMediaAesKey(AgentWechatDisplayFormatter.extractMediaAesKey(message.getContent(),
                message.getRawPayload()));
        respVO.setThumbUrl(AgentWechatDisplayFormatter.extractThumbUrl(message.getContent(), message.getRawPayload()));
        respVO.setMediaName(AgentWechatDisplayFormatter.extractMediaName(message.getContent(), message.getRawPayload()));
        respVO.setMediaDurationMillis(AgentWechatDisplayFormatter.extractVoiceDurationMillis(message.getContent(),
                message.getRawPayload()));
        return respVO;
    }

    private String mediaFileExtension(String contentType) {
        String normalized = StrUtil.blankToDefault(contentType, "").toLowerCase();
        if (normalized.contains("mpeg") || normalized.contains("mp3")) {
            return ".mp3";
        }
        if (normalized.contains("png")) {
            return ".png";
        }
        if (normalized.contains("jpeg") || normalized.contains("jpg")) {
            return ".jpg";
        }
        if (normalized.contains("gif")) {
            return ".gif";
        }
        if (normalized.contains("webp")) {
            return ".webp";
        }
        if (normalized.contains("pdf")) {
            return ".pdf";
        }
        if (normalized.contains("wav")) {
            return ".wav";
        }
        if (normalized.contains("amr")) {
            return ".amr";
        }
        return ".silk";
    }

    private GeweMediaDownloadResult downloadByMessageType(AgentWechatAccountDO account, AgentMessageDO message) {
        Integer messageType = message.getMessageType();
        if (Objects.equals(messageType, AgentConstants.MESSAGE_TYPE_IMAGE)) {
            return geweMessageClient.downloadImage(account,
                    AgentWechatDisplayFormatter.extractMessageXml(message.getContent(), message.getRawPayload()));
        }
        if (Objects.equals(messageType, AgentConstants.MESSAGE_TYPE_EMOJI)) {
            return geweMessageClient.downloadEmoji(account,
                    AgentWechatDisplayFormatter.extractEmojiMd5(message.getContent(), message.getRawPayload()));
        }
        if (Objects.equals(messageType, AgentConstants.MESSAGE_TYPE_FILE_OR_LINK)
                || Objects.equals(messageType, AgentConstants.MESSAGE_TYPE_VIDEO)) {
            return geweMessageClient.downloadFile(account,
                    AgentWechatDisplayFormatter.extractMessageXml(message.getContent(), message.getRawPayload()));
        }
        return GeweMediaDownloadResult.failure("该消息类型不支持 Gewe 媒体下载", null);
    }

    private String contentDisposition(AgentMessageDO message, String contentType, Long messageId) {
        boolean inline = Objects.equals(message.getMessageType(), AgentConstants.MESSAGE_TYPE_IMAGE)
                || Objects.equals(message.getMessageType(), AgentConstants.MESSAGE_TYPE_EMOJI)
                || Objects.equals(message.getMessageType(), AgentConstants.MESSAGE_TYPE_VIDEO);
        String fileName = AgentWechatDisplayFormatter.extractMediaName(message.getContent(), message.getRawPayload());
        fileName = StrUtil.blankToDefault(fileName, "wechat-media-" + messageId + mediaFileExtension(contentType));
        return (inline ? "inline" : "attachment") + "; filename=\"" + sanitizeFileName(fileName) + "\"";
    }

    private String sanitizeFileName(String fileName) {
        return StrUtil.blankToDefault(fileName, "wechat-media")
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\"", "_")
                .replace("\r", "")
                .replace("\n", "");
    }

    private Map<String, Object> accountBrief(AgentWechatAccountDO account) {
        if (account == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", account.getId());
        map.put("nickname", AgentWechatDisplayFormatter.cleanScalar(account.getNickname()));
        map.put("wechatId", AgentWechatDisplayFormatter.cleanScalar(account.getWechatId()));
        map.put("geweCredentialId", account.getGeweCredentialId());
        map.put("geweAppId", account.getGeweAppId());
        map.put("loginStatus", account.getLoginStatus());
        map.put("status", account.getStatus());
        return map;
    }

    private Map<String, Object> contactBrief(AgentWechatContactDO contact) {
        if (contact == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", contact.getId());
        map.put("wechatAccountId", contact.getWechatAccountId());
        map.put("externalUserId", AgentWechatDisplayFormatter.cleanScalar(contact.getExternalUserId()));
        map.put("wechatId", AgentWechatDisplayFormatter.cleanScalar(contact.getWechatId()));
        map.put("nickname", AgentWechatDisplayFormatter.cleanScalar(contact.getNickname()));
        map.put("remark", AgentWechatDisplayFormatter.cleanScalar(contact.getRemark()));
        map.put("riskLevel", contact.getRiskLevel());
        map.put("replyMode", contact.getReplyMode());
        map.put("purchaseIntention", contact.getPurchaseIntention());
        map.put("salesStage", contact.getSalesStage());
        map.put("customerSentiment", contact.getCustomerSentiment());
        map.put("followUpPriority", contact.getFollowUpPriority());
        return map;
    }

    @PostMapping("/send-message")
    @Operation(summary = "发送人工消息")
    @PreAuthorize("@ss.hasPermission('agent:conversation:send')")
    public CommonResult<Long> sendMessage(@Valid @RequestBody AgentConversationSendMessageReqVO sendReqVO) {
        return success(conversationService.sendMessage(sendReqVO));
    }

    @PostMapping("/restore-original-policy")
    @Operation(summary = "恢复会话原回复策略")
    @PreAuthorize("@ss.hasPermission('agent:conversation:send')")
    public CommonResult<Boolean> restoreOriginalPolicy(
            @Valid @RequestBody AgentConversationRestoreOriginalPolicyReqVO restoreReqVO) {
        conversationService.restoreOriginalPolicy(restoreReqVO);
        return success(true);
    }

    @PostMapping("/acknowledge-pending")
    @Operation(summary = "Mark pending conversation reminder as viewed")
    @PreAuthorize("@ss.hasPermission('agent:conversation:query')")
    public CommonResult<Boolean> acknowledgePending(
            @Valid @RequestBody AgentConversationRestoreOriginalPolicyReqVO reqVO) {
        return success(conversationService.acknowledgePendingView(reqVO));
    }

    @PostMapping("/contact-settings/save")
    @Operation(summary = "Save customer workbench settings")
    @PreAuthorize("@ss.hasPermission('agent:contact:update') || @ss.hasPermission('agent:conversation:send')")
    public CommonResult<AgentConversationContactSettingsRespVO> saveContactSettings(
            @Valid @RequestBody AgentConversationContactSettingsSaveReqVO saveReqVO) {
        return success(conversationService.saveContactSettings(saveReqVO));
    }

    @PostMapping("/approve-message")
    @Operation(summary = "审核通过 AI 消息并发送")
    @PreAuthorize("@ss.hasPermission('agent:conversation:send')")
    public CommonResult<Boolean> approveMessage(@Valid @RequestBody AgentConversationMessageReviewReqVO reviewReqVO) {
        conversationService.approveMessage(reviewReqVO);
        return success(true);
    }

    @PostMapping("/reject-message")
    @Operation(summary = "驳回 AI 消息")
    @PreAuthorize("@ss.hasPermission('agent:conversation:send')")
    public CommonResult<Boolean> rejectMessage(@Valid @RequestBody AgentConversationMessageReviewReqVO reviewReqVO) {
        conversationService.rejectMessage(reviewReqVO);
        return success(true);
    }

}
