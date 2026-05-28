package cn.ai.sales.module.agent.controller.open;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.framework.tenant.core.aop.TenantIgnore;
import cn.ai.sales.framework.tenant.core.util.TenantUtils;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatAccountPageReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactPageReqVO;
import cn.ai.sales.module.agent.controller.admin.contact.vo.AgentWechatContactUpdateTagsReqVO;
import cn.ai.sales.module.agent.controller.admin.conversation.vo.AgentConversationSendMessageReqVO;
import cn.ai.sales.module.agent.controller.admin.diagnostics.vo.AgentWebhookEventPageReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewApproveReqVO;
import cn.ai.sales.module.agent.controller.admin.review.vo.AgentReplyReviewRejectReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentContactTagDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentConversationDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentReplyDecisionDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConversationMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMessageMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentReplyDecisionMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatAccountMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentWechatContactMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.account.AgentWechatAccountService;
import cn.ai.sales.module.agent.service.contact.AgentWechatContactDisplayService;
import cn.ai.sales.module.agent.service.conversation.AgentConversationService;
import cn.ai.sales.module.agent.service.conversation.AgentWechatDisplayFormatter;
import cn.ai.sales.module.agent.service.diagnostics.AgentDiagnosticsService;
import cn.ai.sales.module.agent.service.gewe.GeweMessageTimeNormalizer;
import cn.ai.sales.module.agent.service.reply.AgentConversationContextBuilder;
import cn.ai.sales.module.agent.service.reply.AgentGeneratedReply;
import cn.ai.sales.module.agent.service.reply.AgentReplyGenerator;
import cn.ai.sales.module.agent.service.review.AgentReplyReviewService;
import cn.ai.sales.module.agent.service.statistics.AgentStatisticsService;
import cn.ai.sales.module.agent.service.tag.AgentContactTagService;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/skills/personal-sales-assistant")
@Validated
public class PersonalSalesAssistantSkillController {

    @Resource
    private PersonalSalesAssistantSkillProperties properties;
    @Resource
    private AgentWechatAccountMapper accountMapper;
    @Resource
    private AgentWechatContactMapper contactMapper;
    @Resource
    private AgentConversationMapper conversationMapper;
    @Resource
    private AgentMessageMapper messageMapper;
    @Resource
    private AgentReplyDecisionMapper decisionMapper;
    @Resource
    private AgentConversationService conversationService;
    @Resource
    private AgentReplyReviewService replyReviewService;
    @Resource
    private AgentConversationContextBuilder contextBuilder;
    @Resource
    private AgentReplyGenerator replyGenerator;
    @Resource
    private AgentWechatAccountService accountService;
    @Resource
    private AgentWechatContactDisplayService contactDisplayService;
    @Resource
    private AgentStatisticsService statisticsService;
    @Resource
    private AgentDiagnosticsService diagnosticsService;
    @Resource
    private AgentContactTagService tagService;

    @GetMapping("/manifest")
    @PermitAll
    @TenantIgnore
    public Map<String, Object> manifest(@RequestParam(value = "key", required = false) String key,
                                        @RequestHeader(value = "X-Personal-Sales-Assistant-Skill-Key", required = false)
                                        String headerKey) {
        authenticate(firstNotBlank(headerKey, key, ""));
        return Map.of("ok", true, "result", Map.of(
                "skill_name", "personal_sales_assistant",
                "display_name", "个人销售助手",
                "endpoint", "/api/v1/skills/personal-sales-assistant/run",
                "actions", List.of(
                        action("service_session_init"),
                        action("wechat_account_status"),
                        action("customer_threads"),
                        action("recent_messages"),
                        action("draft_reply"),
                        action("send_reply"),
                        action("reply_policy_list"),
                        action("reply_policy_set"),
                        action("reply_policy_delete"),
                        action("reply_decision_list"),
                        action("approve_reply"),
                        action("reject_reply"),
                        action("customer_profile_set"),
                        action("tag_list"),
                        action("statistics_summary"),
                        action("diagnostics_summary"),
                        action("webhook_events"))));
    }

    @PostMapping("/run")
    @PermitAll
    @TenantIgnore
    public Map<String, Object> run(@RequestHeader(value = "X-Personal-Sales-Assistant-Skill-Key", required = false)
                                   String headerKey,
                                   @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> request = payload == null ? Map.of() : payload;
        Map<String, Object> data = input(request);
        authenticate(firstNotBlank(headerKey, string(request.get("api_key")), string(request.get("key")),
                string(data.get("api_key")), string(data.get("key"))));
        Long tenantId = Convert.toLong(firstValue(data, "tenant_id", "tenantId"), properties.getDefaultTenantId());
        String action = firstNotBlank(string(request.get("action")), string(request.get("skill_action")),
                string(data.get("action")), string(data.get("skill_action")), "service_session_init");
        Map<String, Object> result = TenantUtils.execute(tenantId, () -> switch (action) {
            case "service_session_init", "wechat_account_status" -> accountStatus();
            case "customer_threads" -> customerThreads(data);
            case "recent_messages" -> recentMessages(data);
            case "draft_reply" -> draftReply(data);
            case "send_reply" -> sendReply(data);
            case "reply_policy_list" -> replyPolicyList(data);
            case "reply_policy_set" -> replyPolicySet(data);
            case "reply_policy_delete" -> replyPolicyDelete(data);
            case "reply_decision_list" -> replyDecisionList(data);
            case "approve_reply" -> approveReply(data);
            case "reject_reply" -> rejectReply(data);
            case "customer_profile_set" -> customerProfileSet(data);
            case "tag_list" -> tagList();
            case "statistics_summary" -> statisticsSummary();
            case "diagnostics_summary" -> diagnosticsSummary();
            case "webhook_events" -> webhookEvents(data);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported skill action: " + action);
        });
        return Map.of("ok", true, "skill", "personal_sales_assistant", "action", action, "result", result);
    }

    private Map<String, Object> accountStatus() {
        AgentWechatAccountPageReqVO reqVO = new AgentWechatAccountPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(properties.getDefaultLimit());
        PageResult<AgentWechatAccountDO> page = accountService.getWechatAccountPage(reqVO);
        List<Map<String, Object>> accounts = page.getList().stream().map(this::account).toList();
        long onlineCount = page.getList().stream()
                .filter(account -> Objects.equals(account.getLoginStatus(), AgentConstants.LOGIN_STATUS_ONLINE))
                .count();
        Map<String, Object> accountStatus = new LinkedHashMap<>();
        accountStatus.put("has_bound_account", page.getTotal() > 0);
        accountStatus.put("has_online_account", onlineCount > 0);
        accountStatus.put("account_count", page.getTotal());
        accountStatus.put("online_account_count", onlineCount);
        return Map.of("account_status", accountStatus,
                "accounts", accounts,
                "total", page.getTotal());
    }

    private Map<String, Object> customerThreads(Map<String, Object> data) {
        AgentWechatContactPageReqVO reqVO = new AgentWechatContactPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(limit(data));
        reqVO.setWechatAccountId(Convert.toLong(firstValue(data, "account_id", "wechatAccountId", "wechat_account_id"), null));
        reqVO.setKeyword(firstNotBlank(string(firstValue(data, "search", "query")), string(data.get("keyword"))));
        reqVO.setQueueType(normalizeQueueType(string(firstValue(data, "queue_type", "queueType"))));
        reqVO.setFollowUpPriority(normalizeFollowUpPriority(string(firstValue(data, "follow_up_priority",
                "followUpPriority"))));
        PageResult<AgentWechatContactDO> page = contactMapper.selectConversationQueuePage(reqVO);
        page.getList().forEach(contactDisplayService::refreshDisplayNameIfNeeded);
        return Map.of("threads", page.getList().stream().map(this::thread).toList(),
                "total", page.getTotal());
    }

    private Map<String, Object> recentMessages(Map<String, Object> data) {
        Long conversationId = Convert.toLong(firstValue(data, "conversation_id", "conversationId", "thread_id", "threadId"), null);
        if (conversationId == null) {
            return Map.of("messages", List.of(), "total", 0);
        }
        List<AgentMessageDO> messages = messageMapper.selectListByConversationId(conversationId);
        int limit = limit(data);
        if (messages.size() > limit) {
            messages = messages.subList(messages.size() - limit, messages.size());
        }
        return Map.of("conversation_id", conversationId,
                "messages", messages.stream().map(this::message).toList(),
                "total", messages.size());
    }

    private Map<String, Object> sendReply(Map<String, Object> data) {
        Long conversationId = Convert.toLong(firstValue(data, "conversation_id", "conversationId", "thread_id", "threadId"), null);
        String content = firstNotBlank(string(firstValue(data, "reply_text", "text", "content")), "");
        if (conversationId == null || StrUtil.isBlank(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversation_id and reply text are required");
        }
        AgentConversationSendMessageReqVO reqVO = new AgentConversationSendMessageReqVO();
        reqVO.setConversationId(conversationId);
        reqVO.setContent(content);
        Long messageId = conversationService.sendMessage(reqVO);
        return Map.of("sent", true, "message_id", messageId);
    }

    private Map<String, Object> draftReply(Map<String, Object> data) {
        Long conversationId = Convert.toLong(firstValue(data, "conversation_id", "conversationId", "thread_id", "threadId"), null);
        if (conversationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversation_id is required");
        }
        AgentConversationDO conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversation does not exist");
        }
        List<AgentMessageDO> messages = messageMapper.selectListByConversationId(conversationId);
        AgentMessageDO inbound = messages.stream()
                .filter(message -> Objects.equals(AgentConstants.MESSAGE_DIRECTION_INBOUND, message.getDirection()))
                .reduce((first, second) -> second)
                .orElse(null);
        if (inbound == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversation has no inbound message");
        }
        AgentWechatAccountDO account = accountMapper.selectById(conversation.getWechatAccountId());
        AgentGeneratedReply generatedReply = replyGenerator.generate(contextBuilder.build(conversationId, inbound, null, account));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversation_id", conversationId);
        result.put("draft", generatedReply.content());
        result.put("confidence", generatedReply.confidence());
        result.put("matched_knowledge_title", generatedReply.matchedKnowledgeTitle());
        result.put("generation_source", generatedReply.generationSource());
        result.put("llm_provider", generatedReply.llmProvider());
        result.put("llm_model", generatedReply.llmModel());
        result.put("manual_confirm_required", true);
        result.put("send_allowed", false);
        return result;
    }

    private Map<String, Object> replyPolicyList(Map<String, Object> data) {
        AgentWechatContactDO contact = resolveContact(data);
        if (contact == null) {
            return Map.of("policies", List.of(), "total", 0);
        }
        AgentWechatAccountDO account = accountMapper.selectById(contact.getWechatAccountId());
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("contact_id", contact.getId());
        policy.put("conversation_id", conversationId(contact));
        policy.put("account_id", contact.getWechatAccountId());
        policy.put("policy_source", contact.getReplyMode() == null ? "ACCOUNT" : "CONTACT");
        policy.put("reply_mode", firstNotBlank(contact.getReplyMode(), account == null ? null : account.getReplyMode(),
                AgentConstants.REPLY_MODE_MANUAL_CONFIRM));
        policy.put("quiet_seconds", contact.getQuietSeconds() == null
                ? account == null ? null : account.getQuietSeconds() : contact.getQuietSeconds());
        policy.put("business_hours", contact.getBusinessHours() == null
                ? account == null ? null : account.getBusinessHours() : contact.getBusinessHours());
        return Map.of("policies", List.of(policy), "total", 1);
    }

    private Map<String, Object> replyPolicySet(Map<String, Object> data) {
        AgentWechatContactDO contact = requireContact(data);
        boolean hasReplyPolicy = firstValue(data, "reply_mode", "policy_mode", "mode", "quiet_seconds",
                "delay_seconds", "auto_reply_delay_seconds", "business_hours", "businessHours") != null;
        AgentWechatContactDO replyPolicyUpdate = null;
        if (hasReplyPolicy) {
            replyPolicyUpdate = new AgentWechatContactDO();
            replyPolicyUpdate.setId(contact.getId());
            replyPolicyUpdate.setReplyMode(normalizeReplyMode(string(firstValue(data, "reply_mode", "policy_mode", "mode"))));
            replyPolicyUpdate.setQuietSeconds(Convert.toInt(firstValue(data, "quiet_seconds", "delay_seconds", "auto_reply_delay_seconds"),
                    contact.getQuietSeconds()));
            replyPolicyUpdate.setBusinessHours(readMap(firstValue(data, "business_hours", "businessHours")));
            contactMapper.updateReplyPolicyById(replyPolicyUpdate);
        }
        String followUpPriority = normalizeImportantPriority(data, contact);
        boolean priorityUpdated = false;
        if (followUpPriority != null && !Objects.equals(followUpPriority, contact.getFollowUpPriority())) {
            AgentWechatContactDO priorityUpdate = new AgentWechatContactDO();
            priorityUpdate.setId(contact.getId());
            priorityUpdate.setFollowUpPriority(followUpPriority);
            contactMapper.updateById(priorityUpdate);
            contact.setFollowUpPriority(followUpPriority);
            priorityUpdated = true;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", hasReplyPolicy || priorityUpdated);
        result.put("contact_id", contact.getId());
        result.put("reply_mode", replyPolicyUpdate == null ? contact.getReplyMode() : replyPolicyUpdate.getReplyMode());
        result.put("quiet_seconds", replyPolicyUpdate == null ? contact.getQuietSeconds() : replyPolicyUpdate.getQuietSeconds());
        result.put("follow_up_priority", contact.getFollowUpPriority());
        result.put("important", Objects.equals(contact.getFollowUpPriority(), AgentConstants.FOLLOW_UP_PRIORITY_FOCUS));
        return result;
    }

    private Map<String, Object> replyPolicyDelete(Map<String, Object> data) {
        AgentWechatContactDO contact = requireContact(data);
        AgentWechatContactDO update = new AgentWechatContactDO();
        update.setId(contact.getId());
        contactMapper.updateReplyPolicyById(update);
        return Map.of("deleted", true, "contact_id", contact.getId(), "policy_source", "ACCOUNT");
    }

    private Map<String, Object> replyDecisionList(Map<String, Object> data) {
        Long conversationId = Convert.toLong(firstValue(data, "conversation_id", "conversationId", "thread_id", "threadId"), null);
        String reviewStatus = firstNotBlank(string(firstValue(data, "review_status", "status")),
                AgentConstants.REVIEW_STATUS_PENDING);
        int limit = limit(data);
        List<AgentReplyDecisionDO> decisions = decisionMapper.selectList(new LambdaQueryWrapperX<AgentReplyDecisionDO>()
                .eqIfPresent(AgentReplyDecisionDO::getConversationId, conversationId)
                .eqIfPresent(AgentReplyDecisionDO::getReviewStatus, reviewStatus)
                .orderByDesc(AgentReplyDecisionDO::getId)
                .last("LIMIT " + limit));
        return Map.of("decisions", decisions.stream().map(this::decision).toList(),
                "total", decisions.size());
    }

    private Map<String, Object> approveReply(Map<String, Object> data) {
        Long decisionId = Convert.toLong(firstValue(data, "decision_id", "decisionId"), null);
        if (decisionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "decision_id is required");
        }
        AgentReplyReviewApproveReqVO reqVO = new AgentReplyReviewApproveReqVO();
        reqVO.setDecisionId(decisionId);
        reqVO.setContent(string(firstValue(data, "reply_text", "content", "text")));
        Long messageId = replyReviewService.approve(reqVO);
        return Map.of("approved", true, "decision_id", decisionId, "message_id", messageId);
    }

    private Map<String, Object> rejectReply(Map<String, Object> data) {
        Long decisionId = Convert.toLong(firstValue(data, "decision_id", "decisionId"), null);
        if (decisionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "decision_id is required");
        }
        AgentReplyReviewRejectReqVO reqVO = new AgentReplyReviewRejectReqVO();
        reqVO.setDecisionId(decisionId);
        reqVO.setReason(firstNotBlank(string(firstValue(data, "reason", "note")), "人工驳回"));
        replyReviewService.reject(reqVO);
        return Map.of("rejected", true, "decision_id", decisionId);
    }

    private Map<String, Object> customerProfileSet(Map<String, Object> data) {
        AgentWechatContactDO contact = requireContact(data);
        AgentWechatContactDO update = new AgentWechatContactDO();
        update.setId(contact.getId());
        boolean changed = false;
        if (firstValue(data, "customer_level", "customerLevel") != null) {
            update.setCustomerLevel(Convert.toInt(firstValue(data, "customer_level", "customerLevel"), contact.getCustomerLevel()));
            contact.setCustomerLevel(update.getCustomerLevel());
            changed = true;
        }
        String purchaseIntention = normalizeCode(string(firstValue(data, "purchase_intention", "purchaseIntention")));
        if (purchaseIntention != null) {
            update.setPurchaseIntention(purchaseIntention);
            contact.setPurchaseIntention(purchaseIntention);
            changed = true;
        }
        String salesStage = normalizeCode(string(firstValue(data, "sales_stage", "salesStage")));
        if (salesStage != null) {
            update.setSalesStage(salesStage);
            contact.setSalesStage(salesStage);
            changed = true;
        }
        String customerSentiment = normalizeCode(string(firstValue(data, "customer_sentiment", "customerSentiment")));
        if (customerSentiment != null) {
            update.setCustomerSentiment(customerSentiment);
            contact.setCustomerSentiment(customerSentiment);
            changed = true;
        }
        String followUpPriority = normalizeFollowUpPriority(string(firstValue(data, "follow_up_priority",
                "followUpPriority")));
        if (followUpPriority != null) {
            update.setFollowUpPriority(followUpPriority);
            contact.setFollowUpPriority(followUpPriority);
            changed = true;
        }
        if (changed) {
            contactMapper.updateById(update);
        }
        Object tagValue = firstValue(data, "tag_ids", "tagIds");
        List<Long> tagIds = longList(tagValue);
        boolean tagsChanged = tagIds != null;
        if (tagsChanged) {
            AgentWechatContactUpdateTagsReqVO reqVO = new AgentWechatContactUpdateTagsReqVO();
            reqVO.setContactId(contact.getId());
            reqVO.setTagIds(tagIds);
            tagService.updateContactTags(reqVO);
        }
        return Map.of("updated", changed || tagsChanged,
                "contact_id", contact.getId(),
                "tag_ids", tagIds == null ? List.of() : tagIds,
                "thread", thread(contact));
    }

    private Map<String, Object> tagList() {
        List<Map<String, Object>> tags = tagService.getEnabledTags().stream().map(this::tag).toList();
        return Map.of("tags", tags, "total", tags.size());
    }

    private Map<String, Object> statisticsSummary() {
        return Map.of("summary", statisticsService.getSummary());
    }

    private Map<String, Object> diagnosticsSummary() {
        return Map.of("summary", diagnosticsService.getSummary());
    }

    private Map<String, Object> webhookEvents(Map<String, Object> data) {
        AgentWebhookEventPageReqVO reqVO = new AgentWebhookEventPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(limit(data));
        reqVO.setWechatAccountId(Convert.toLong(firstValue(data, "account_id", "wechatAccountId", "wechat_account_id"), null));
        reqVO.setEventId(string(firstValue(data, "event_id", "eventId")));
        reqVO.setEventType(string(firstValue(data, "event_type", "eventType")));
        reqVO.setProcessStatus(Convert.toInt(firstValue(data, "process_status", "processStatus"), null));
        PageResult<?> page = diagnosticsService.getWebhookEventPage(reqVO);
        return Map.of("events", page.getList(), "total", page.getTotal());
    }

    private Map<String, Object> account(AgentWechatAccountDO account) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", account.getId());
        map.put("nickname", account.getNickname());
        map.put("display_name", firstNotBlank(account.getNickname(), account.getWechatId(), account.getGeweAppId()));
        map.put("wechat_id", account.getWechatId());
        map.put("wxid", account.getWechatId());
        map.put("gewe_app_id", account.getGeweAppId());
        map.put("login_status", formatLoginStatus(account.getLoginStatus()));
        map.put("login_status_code", account.getLoginStatus());
        map.put("is_online", Objects.equals(account.getLoginStatus(), AgentConstants.LOGIN_STATUS_ONLINE));
        map.put("status", account.getStatus());
        map.put("callback_url", account.getCallbackUrl());
        map.put("callback_status", account.getStatus() == null
                || Objects.equals(account.getStatus(), AgentConstants.STATUS_ENABLE) ? "enabled" : "disabled");
        map.put("can_receive_messages", Objects.equals(account.getLoginStatus(), AgentConstants.LOGIN_STATUS_ONLINE));
        map.put("can_send_reply", Objects.equals(account.getLoginStatus(), AgentConstants.LOGIN_STATUS_ONLINE)
                && (account.getStatus() == null || Objects.equals(account.getStatus(), AgentConstants.STATUS_ENABLE)));
        map.put("last_heartbeat_time", account.getLastHeartbeatTime());
        return map;
    }

    private String formatLoginStatus(Integer loginStatus) {
        if (Objects.equals(loginStatus, AgentConstants.LOGIN_STATUS_ONLINE)) {
            return "online";
        }
        if (Objects.equals(loginStatus, AgentConstants.LOGIN_STATUS_OFFLINE)) {
            return "offline";
        }
        if (Objects.equals(loginStatus, AgentConstants.LOGIN_STATUS_EXPIRED)) {
            return "expired";
        }
        return "unknown";
    }

    private Map<String, Object> thread(AgentWechatContactDO contact) {
        AgentConversationDO conversation = conversationMapper.selectByAccountAndContact(contact.getWechatAccountId(),
                contact.getId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contact_id", contact.getId());
        map.put("thread_id", conversation == null ? null : conversation.getId());
        map.put("account_id", contact.getWechatAccountId());
        String displayName = contactDisplayService.resolveDisplayName(contact);
        map.put("customer_name", displayName);
        map.put("display_name", displayName);
        map.put("nickname", AgentWechatDisplayFormatter.cleanScalar(contact.getNickname()));
        map.put("remark", AgentWechatDisplayFormatter.cleanScalar(contact.getRemark()));
        map.put("customer_wxid", contact.getExternalUserId());
        map.put("last_message_time", GeweMessageTimeNormalizer.normalize(contact.getLastMessageTime()));
        map.put("reply_mode", contact.getReplyMode());
        map.put("risk_level", contact.getRiskLevel());
        map.put("purchase_intention", contact.getPurchaseIntention());
        map.put("sales_stage", contact.getSalesStage());
        map.put("customer_sentiment", contact.getCustomerSentiment());
        map.put("follow_up_priority", contact.getFollowUpPriority());
        return map;
    }

    private Map<String, Object> message(AgentMessageDO message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", message.getId());
        map.put("conversation_id", message.getConversationId());
        map.put("direction", message.getDirection());
        map.put("sender_type", message.getSenderType());
        map.put("message_type", message.getMessageType());
        map.put("content", message.getContent());
        map.put("send_status", message.getSendStatus());
        map.put("message_time", message.getMessageTime());
        return map;
    }

    private Map<String, Object> decision(AgentReplyDecisionDO decision) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", decision.getId());
        map.put("conversation_id", decision.getConversationId());
        map.put("inbound_message_id", decision.getInboundMessageId());
        map.put("suggested_message_id", decision.getSuggestedMessageId());
        map.put("sent_message_id", decision.getSentMessageId());
        map.put("decision_type", decision.getDecisionType());
        map.put("suggested_action", decision.getDecisionType() == null
                ? AgentConstants.DECISION_TYPE_MANUAL_CONFIRM : decision.getDecisionType());
        map.put("policy_mode", decision.getDecisionType() == null
                ? AgentConstants.REPLY_MODE_MANUAL_CONFIRM : decision.getDecisionType());
        map.put("risk_level", decision.getRiskLevel());
        map.put("confidence", decision.getConfidence());
        map.put("llm_model", decision.getLlmModel());
        map.put("knowledge_refs", decision.getKnowledgeRefs());
        if (decision.getKnowledgeRefs() != null) {
            map.put("generation_source", decision.getKnowledgeRefs().get("generationSource"));
            map.put("llm_provider", decision.getKnowledgeRefs().get("llmProvider"));
            map.put("actual_llm_model", decision.getKnowledgeRefs().get("llmModel"));
        }
        map.put("decision_reason", decision.getDecisionReason());
        map.put("review_status", decision.getReviewStatus());
        map.put("review_note", decision.getReviewNote());
        map.put("create_time", decision.getCreateTime());
        if (decision.getSuggestedMessageId() != null) {
            AgentMessageDO suggested = messageMapper.selectById(decision.getSuggestedMessageId());
            if (suggested != null) {
                map.put("suggested_content", suggested.getContent());
                map.put("send_status", suggested.getSendStatus());
            }
        }
        return map;
    }

    private Map<String, Object> tag(AgentContactTagDO tag) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tag.getId());
        map.put("name", tag.getName());
        map.put("color", tag.getColor());
        map.put("description", tag.getDescription());
        map.put("sort", tag.getSort());
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> input(Map<String, Object> payload) {
        Object input = payload.get("input");
        if (!(input instanceof Map<?, ?>)) {
            input = payload.get("parameters");
        }
        return input instanceof Map<?, ?> map ? (Map<String, Object>) map : payload;
    }

    private int limit(Map<String, Object> data) {
        return Math.max(1, Math.min(100, Convert.toInt(firstValue(data, "limit", "pageSize"),
                properties.getDefaultLimit())));
    }

    private AgentWechatContactDO resolveContact(Map<String, Object> data) {
        Long contactId = Convert.toLong(firstValue(data, "contact_id", "contactId", "customer_id", "customerId"), null);
        if (contactId != null) {
            return contactMapper.selectById(contactId);
        }
        Long conversationId = Convert.toLong(firstValue(data, "conversation_id", "conversationId", "thread_id", "threadId"), null);
        if (conversationId == null) {
            return null;
        }
        AgentConversationDO conversation = conversationMapper.selectById(conversationId);
        return conversation == null ? null : contactMapper.selectById(conversation.getContactId());
    }

    private AgentWechatContactDO requireContact(Map<String, Object> data) {
        AgentWechatContactDO contact = resolveContact(data);
        if (contact == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contact_id or conversation_id is required");
        }
        return contact;
    }

    private Long conversationId(AgentWechatContactDO contact) {
        AgentConversationDO conversation = conversationMapper.selectByAccountAndContact(contact.getWechatAccountId(),
                contact.getId());
        return conversation == null ? null : conversation.getId();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private String normalizeReplyMode(String replyMode) {
        if (AgentConstants.REPLY_MODE_MANUAL_ONLY.equals(replyMode)) {
            return AgentConstants.REPLY_MODE_MANUAL_CONFIRM;
        }
        if (StrUtil.isBlank(replyMode)) {
            return AgentConstants.REPLY_MODE_MANUAL_CONFIRM;
        }
        return replyMode;
    }

    private String normalizeQueueType(String queueType) {
        if (StrUtil.isBlank(queueType)) {
            return null;
        }
        String normalized = queueType.trim().toUpperCase();
        if ("IMPORTANT".equals(normalized) || "IMPORTANT_CUSTOMERS".equals(normalized)) {
            return AgentConstants.CONVERSATION_QUEUE_FOCUS;
        }
        return normalized;
    }

    private String normalizeFollowUpPriority(String priority) {
        if (StrUtil.isBlank(priority)) {
            return null;
        }
        String normalized = priority.trim().toUpperCase();
        if ("IMPORTANT".equals(normalized)) {
            return AgentConstants.FOLLOW_UP_PRIORITY_FOCUS;
        }
        return normalized;
    }

    private String normalizeCode(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String normalizeImportantPriority(Map<String, Object> data, AgentWechatContactDO contact) {
        Object important = firstValue(data, "important", "is_important", "isImportant");
        if (important != null) {
            boolean importantValue = Boolean.parseBoolean(String.valueOf(important));
            if (!importantValue && !Objects.equals(contact.getFollowUpPriority(), AgentConstants.FOLLOW_UP_PRIORITY_FOCUS)) {
                return contact.getFollowUpPriority();
            }
            return importantValue ? AgentConstants.FOLLOW_UP_PRIORITY_FOCUS : AgentConstants.FOLLOW_UP_PRIORITY_NORMAL;
        }
        return normalizeFollowUpPriority(string(firstValue(data, "follow_up_priority", "followUpPriority")));
    }

    private List<Long> longList(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(item -> Convert.toLong(item, null)).filter(Objects::nonNull).toList();
        }
        String text = string(value);
        if (StrUtil.isBlank(text)) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
                .map(item -> Convert.toLong(item.trim(), null))
                .filter(Objects::nonNull)
                .toList();
    }

    private void authenticate(String key) {
        if (StrUtil.isBlank(properties.getApiKey())) {
            return;
        }
        if (!StrUtil.equals(properties.getApiKey(), key)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid skill key");
        }
    }

    private Map<String, Object> action(String action) {
        return Map.of("action", action);
    }

    private Object firstValue(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null) {
                return value;
            }
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

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
