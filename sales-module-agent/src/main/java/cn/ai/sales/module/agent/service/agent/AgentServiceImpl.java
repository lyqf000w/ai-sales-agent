package cn.ai.sales.module.agent.service.agent;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentLlmModelOptionRespVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentPageReqVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentPublishReqVO;
import cn.ai.sales.module.agent.controller.admin.agent.vo.AgentSaveReqVO;
import cn.ai.sales.framework.security.core.util.SecurityFrameworkUtils;
import cn.ai.sales.module.agent.dal.dataobject.AgentConfigVersionDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentDO;
import cn.ai.sales.module.agent.dal.mysql.AgentConfigVersionMapper;
import cn.ai.sales.module.agent.dal.mysql.AgentMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.AGENT_LLM_MODEL_INVALID;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.AGENT_NAME_DUPLICATE;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.AGENT_NOT_EXISTS;

@Service
@Validated
public class AgentServiceImpl implements AgentService {

    private static final String DEFAULT_LLM_PROVIDER = "DEEPSEEK";
    private static final String DEFAULT_LLM_MODEL = "deepseek-v4-pro";
    private static final List<AgentLlmModelOptionRespVO> LLM_MODEL_OPTIONS = List.of(
            new AgentLlmModelOptionRespVO(DEFAULT_LLM_PROVIDER, DEFAULT_LLM_MODEL, "DeepSeek-V4-Pro", true),
            new AgentLlmModelOptionRespVO(DEFAULT_LLM_PROVIDER, "deepseek-v4-flash", "DeepSeek-V4-Flash", false)
    );

    @Resource
    private AgentMapper agentMapper;
    @Resource
    private AgentConfigVersionMapper configVersionMapper;

    @Override
    public Long createAgent(AgentSaveReqVO createReqVO) {
        validateAgentNameDuplicate(createReqVO.getName(), null);
        AgentDO agent = BeanUtils.toBean(createReqVO, AgentDO.class);
        agent.setDraftVersion(1);
        agent.setOnlineVersion(0);
        fillAgentDefaults(agent);
        agentMapper.insert(agent);
        return agent.getId();
    }

    @Override
    public void updateAgent(AgentSaveReqVO updateReqVO) {
        AgentDO existed = validateAgentExists(updateReqVO.getId());
        validateAgentNameDuplicate(updateReqVO.getName(), updateReqVO.getId());
        AgentDO agent = BeanUtils.toBean(updateReqVO, AgentDO.class);
        fillAgentDefaults(agent);
        if (hasPublishedDraft(existed)) {
            agent.setDraftVersion(existed.getOnlineVersion() + 1);
        }
        agentMapper.updateById(agent);
    }

    @Override
    public void publishAgent(AgentPublishReqVO publishReqVO) {
        AgentDO agent = validateAgentExists(publishReqVO.getAgentId());
        int draftVersion = agent.getDraftVersion() == null ? 1 : agent.getDraftVersion();
        Map<String, Object> snapshot = buildConfigSnapshot(agent, draftVersion);

        AgentConfigVersionDO version = new AgentConfigVersionDO();
        version.setAgentId(agent.getId());
        version.setVersion(draftVersion);
        version.setConfigSnapshot(snapshot);
        version.setChangeSummary(StrUtil.maxLength(publishReqVO.getChangeSummary(), 512));
        version.setPublishUserId(getLoginUserIdQuietly());
        version.setPublishTime(LocalDateTime.now());
        AgentConfigVersionDO existedVersion = configVersionMapper.selectByAgentIdAndVersion(agent.getId(), draftVersion);
        if (existedVersion == null) {
            configVersionMapper.insert(version);
        } else {
            version.setId(existedVersion.getId());
            configVersionMapper.updateById(version);
        }

        AgentDO update = new AgentDO();
        update.setId(agent.getId());
        update.setOnlineVersion(draftVersion);
        update.setPublishedConfig(snapshot);
        agentMapper.updateById(update);
    }

    @Override
    public void deleteAgent(Long id) {
        validateAgentExists(id);
        agentMapper.deleteById(id);
    }

    @Override
    public AgentDO getAgent(Long id) {
        return agentMapper.selectById(id);
    }

    @Override
    public PageResult<AgentDO> getAgentPage(AgentPageReqVO pageReqVO) {
        return agentMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AgentDO> getEnabledAgents() {
        return agentMapper.selectEnabledList();
    }

    @Override
    public List<AgentLlmModelOptionRespVO> getLlmModelOptions() {
        return LLM_MODEL_OPTIONS.stream()
                .sorted(Comparator.comparing(AgentLlmModelOptionRespVO::getDefaultModel).reversed()
                        .thenComparing(AgentLlmModelOptionRespVO::getLabel))
                .toList();
    }

    @Override
    public List<AgentConfigVersionDO> getConfigVersions(Long agentId) {
        validateAgentExists(agentId);
        return configVersionMapper.selectListByAgentId(agentId);
    }

    private AgentDO validateAgentExists(Long id) {
        AgentDO agent = agentMapper.selectById(id);
        if (agent == null) {
            throw exception(AGENT_NOT_EXISTS);
        }
        return agent;
    }

    private void validateAgentNameDuplicate(String name, Long id) {
        AgentDO agent = agentMapper.selectByName(name);
        if (agent == null) {
            return;
        }
        if (id == null || !Objects.equals(agent.getId(), id)) {
            throw exception(AGENT_NAME_DUPLICATE);
        }
    }

    private void fillAgentDefaults(AgentDO agent) {
        if (StrUtil.isBlank(agent.getLlmProvider())) {
            agent.setLlmProvider(DEFAULT_LLM_PROVIDER);
        }
        if (StrUtil.isBlank(agent.getLlmModel())) {
            agent.setLlmModel(DEFAULT_LLM_MODEL);
        }
        validateLlmModel(agent.getLlmProvider(), agent.getLlmModel());
        if (AgentConstants.REPLY_MODE_MANUAL_ONLY.equals(agent.getReplyMode())) {
            agent.setReplyMode(AgentConstants.REPLY_MODE_MANUAL_CONFIRM);
        }
        if (agent.getReplyMode() == null) {
            agent.setReplyMode(AgentConstants.REPLY_MODE_MANUAL_CONFIRM);
        }
        if (agent.getQuietSeconds() == null) {
            agent.setQuietSeconds(agent.getQuietMinutes() == null ? 0 : agent.getQuietMinutes() * 60);
        }
    }

    private void validateLlmModel(String provider, String model) {
        boolean supported = LLM_MODEL_OPTIONS.stream()
                .anyMatch(option -> StrUtil.equalsIgnoreCase(option.getProvider(), provider)
                        && StrUtil.equals(option.getModel(), model));
        if (!supported) {
            throw exception(AGENT_LLM_MODEL_INVALID);
        }
    }

    private boolean hasPublishedDraft(AgentDO agent) {
        return agent.getDraftVersion() != null && agent.getOnlineVersion() != null
                && agent.getOnlineVersion() > 0 && agent.getDraftVersion() <= agent.getOnlineVersion();
    }

    private Map<String, Object> buildConfigSnapshot(AgentDO agent, Integer draftVersion) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", agent.getId());
        snapshot.put("name", agent.getName());
        snapshot.put("aliasName", agent.getAliasName());
        snapshot.put("ownerUserId", agent.getOwnerUserId());
        snapshot.put("scene", agent.getScene());
        snapshot.put("targetCustomerDesc", agent.getTargetCustomerDesc());
        snapshot.put("systemPrompt", agent.getSystemPrompt());
        snapshot.put("llmProvider", agent.getLlmProvider());
        snapshot.put("llmModel", agent.getLlmModel());
        snapshot.put("knowledgeBaseId", agent.getKnowledgeBaseId());
        snapshot.put("replyMode", agent.getReplyMode());
        snapshot.put("quietSeconds", agent.getQuietSeconds());
        snapshot.put("businessHours", agent.getBusinessHours());
        snapshot.put("tone", agent.getTone());
        snapshot.put("welcomeMessage", agent.getWelcomeMessage());
        snapshot.put("handoverMessage", agent.getHandoverMessage());
        snapshot.put("followUpPolicy", agent.getFollowUpPolicy());
        snapshot.put("materialPriority", agent.getMaterialPriority());
        snapshot.put("status", agent.getStatus());
        snapshot.put("draftVersion", draftVersion);
        snapshot.put("onlineVersion", agent.getOnlineVersion());
        return snapshot;
    }

    private Long getLoginUserIdQuietly() {
        try {
            return SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

}
