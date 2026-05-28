package cn.ai.sales.module.agent.service.sensitiverule;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.common.util.object.BeanUtils;
import cn.ai.sales.module.agent.controller.admin.sensitiverule.vo.AgentSensitiveRuleOptionsRespVO;
import cn.ai.sales.module.agent.controller.admin.sensitiverule.vo.AgentSensitiveRulePageReqVO;
import cn.ai.sales.module.agent.controller.admin.sensitiverule.vo.AgentSensitiveRuleSaveReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentSensitiveRuleDO;
import cn.ai.sales.module.agent.dal.mysql.AgentSensitiveRuleMapper;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static cn.ai.sales.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.SENSITIVE_RULE_NAME_DUPLICATE;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.SENSITIVE_RULE_NOT_EXISTS;
import static cn.ai.sales.module.agent.enums.ErrorCodeConstants.SENSITIVE_RULE_PATTERN_INVALID;

@Service
@Validated
public class AgentSensitiveRuleServiceImpl implements AgentSensitiveRuleService {

    private static final AgentSensitiveRuleOptionsRespVO RULE_OPTIONS = new AgentSensitiveRuleOptionsRespVO(
            List.of(
                    option(AgentConstants.ESCALATION_TRIGGER_INTENT, "意图识别",
                            "由 Agent 判断退款、投诉、合同、法务等销售意图"),
                    option(AgentConstants.ESCALATION_TRIGGER_SENTIMENT, "客户情绪",
                            "识别生气、负面等强情绪"),
                    option(AgentConstants.ESCALATION_TRIGGER_KEYWORD, "关键词",
                            "直接匹配客户消息或 AI 草稿中的关键词"),
                    option(AgentConstants.ESCALATION_TRIGGER_REGEX, "正则表达式",
                            "用于更精确的文本匹配"),
                    option(AgentConstants.ESCALATION_TRIGGER_CUSTOMER_LEVEL, "客户等级",
                            "目标客户、重要客户可强制人工确认"),
                    option(AgentConstants.ESCALATION_TRIGGER_RAG_MISS, "知识库未命中",
                            "有知识库但没有命中条目时升级"),
                    option(AgentConstants.ESCALATION_TRIGGER_REQUEST_HUMAN, "要求人工",
                            "客户明确要求真人、负责人、经理等")
            ),
            List.of(
                    option(String.valueOf(AgentConstants.SENSITIVE_ACTION_REVIEW), "人工确认",
                            "生成草稿，等待人工确认后发送"),
                    option(String.valueOf(AgentConstants.SENSITIVE_ACTION_TAKEOVER), "人工接管",
                            "会话转人工，暂停自动回复")
            ),
            List.of(
                    option(String.valueOf(AgentConstants.RISK_LEVEL_GREEN), "绿色", "低风险"),
                    option(String.valueOf(AgentConstants.RISK_LEVEL_YELLOW), "黄色", "需要关注"),
                    option(String.valueOf(AgentConstants.RISK_LEVEL_RED), "红色", "高风险")
            ),
            List.of(
                    option("refund_or_complaint", "退款/投诉/赔偿", "客户表达退款、投诉、赔偿、差评或强烈不满"),
                    option("legal_compliance", "法务/合规/高风险行业", "客户涉及法务、律师、监管、医疗、金融等话题"),
                    option("human_request", "要求人工", "客户要求真人、负责人、老板或销售介入"),
                    option("knowledge_gap", "知识缺口", "客户质疑专业回答或知识库命中"),
                    option("key_customer_project", "重点客户/项目推进", "客户涉及重点客户、大客户项目、招采流程"),
                    option("special_commitment", "合同/特殊承诺", "客户涉及合同、承诺、保证、独家、赔付等"),
                    option("price_consultation", "价格咨询", "客户咨询价格、报价、费用"),
                    option("general_consultation", "普通咨询", "普通销售咨询")
            ),
            List.of(
                    option("angry", "愤怒", "强烈负面情绪"),
                    option("negative", "负面", "一般负面情绪"),
                    option("neutral", "中性", "中性表达")
            ),
            List.of(
                    option(String.valueOf(AgentConstants.CUSTOMER_LEVEL_NORMAL), "普通客户", "普通客户"),
                    option(String.valueOf(AgentConstants.CUSTOMER_LEVEL_TARGET), "目标客户", "目标客户"),
                    option(String.valueOf(AgentConstants.CUSTOMER_LEVEL_IMPORTANT), "重要客户", "重要客户")
            )
    );

    @Resource
    private AgentSensitiveRuleMapper ruleMapper;

    @Override
    public Long createRule(AgentSensitiveRuleSaveReqVO createReqVO) {
        validateRuleNameDuplicate(createReqVO.getName(), null);
        AgentSensitiveRuleDO rule = BeanUtils.toBean(createReqVO, AgentSensitiveRuleDO.class);
        fillRuleDefaults(rule);
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    public void updateRule(AgentSensitiveRuleSaveReqVO updateReqVO) {
        validateRuleExists(updateReqVO.getId());
        validateRuleNameDuplicate(updateReqVO.getName(), updateReqVO.getId());
        AgentSensitiveRuleDO rule = BeanUtils.toBean(updateReqVO, AgentSensitiveRuleDO.class);
        fillRuleDefaults(rule);
        ruleMapper.updateById(rule);
    }

    @Override
    public void deleteRule(Long id) {
        validateRuleExists(id);
        ruleMapper.deleteById(id);
    }

    @Override
    public AgentSensitiveRuleDO getRule(Long id) {
        return ruleMapper.selectById(id);
    }

    @Override
    public PageResult<AgentSensitiveRuleDO> getRulePage(AgentSensitiveRulePageReqVO pageReqVO) {
        return ruleMapper.selectPage(pageReqVO);
    }

    @Override
    public AgentSensitiveRuleOptionsRespVO getRuleOptions() {
        return RULE_OPTIONS;
    }

    private void validateRuleExists(Long id) {
        if (ruleMapper.selectById(id) == null) {
            throw exception(SENSITIVE_RULE_NOT_EXISTS);
        }
    }

    private void validateRuleNameDuplicate(String name, Long id) {
        AgentSensitiveRuleDO rule = ruleMapper.selectByName(name);
        if (rule == null) {
            return;
        }
        if (id == null || !Objects.equals(rule.getId(), id)) {
            throw exception(SENSITIVE_RULE_NAME_DUPLICATE);
        }
    }

    private void fillRuleDefaults(AgentSensitiveRuleDO rule) {
        if (StrUtil.isBlank(rule.getTriggerType())) {
            rule.setTriggerType(toTriggerType(rule.getMatchType()));
        }
        rule.setPattern(StrUtil.trim(StrUtil.blankToDefault(rule.getPattern(), "")));
        if (rule.getMatchType() == null) {
            rule.setMatchType(toMatchType(rule.getTriggerType()));
        }
        validatePattern(rule);
        if (rule.getAction() == null) {
            rule.setAction(AgentConstants.SENSITIVE_ACTION_REVIEW);
        }
        if (rule.getRiskLevel() == null) {
            rule.setRiskLevel(AgentConstants.RISK_LEVEL_YELLOW);
        }
        if (rule.getStatus() == null) {
            rule.setStatus(AgentConstants.STATUS_ENABLE);
        }
        if (rule.getSort() == null) {
            rule.setSort(0);
        }
    }

    private String toTriggerType(Integer matchType) {
        if (AgentConstants.SENSITIVE_MATCH_REGEX == nullToZero(matchType)) {
            return AgentConstants.ESCALATION_TRIGGER_REGEX;
        }
        if (AgentConstants.SENSITIVE_MATCH_LLM_CLASSIFIER == nullToZero(matchType)) {
            return AgentConstants.ESCALATION_TRIGGER_INTENT;
        }
        return AgentConstants.ESCALATION_TRIGGER_KEYWORD;
    }

    private Integer toMatchType(String triggerType) {
        if (AgentConstants.ESCALATION_TRIGGER_REGEX.equals(triggerType)) {
            return AgentConstants.SENSITIVE_MATCH_REGEX;
        }
        if (AgentConstants.ESCALATION_TRIGGER_INTENT.equals(triggerType)
                || AgentConstants.ESCALATION_TRIGGER_SENTIMENT.equals(triggerType)
                || AgentConstants.ESCALATION_TRIGGER_CUSTOMER_LEVEL.equals(triggerType)
                || AgentConstants.ESCALATION_TRIGGER_RAG_MISS.equals(triggerType)
                || AgentConstants.ESCALATION_TRIGGER_REQUEST_HUMAN.equals(triggerType)) {
            return AgentConstants.SENSITIVE_MATCH_LLM_CLASSIFIER;
        }
        return AgentConstants.SENSITIVE_MATCH_KEYWORD;
    }

    private void validatePattern(AgentSensitiveRuleDO rule) {
        String triggerType = rule.getTriggerType();
        if (AgentConstants.ESCALATION_TRIGGER_REQUEST_HUMAN.equals(triggerType)
                || AgentConstants.ESCALATION_TRIGGER_RAG_MISS.equals(triggerType)) {
            rule.setPattern("");
            return;
        }
        if (StrUtil.isBlank(rule.getPattern())) {
            throw exception(SENSITIVE_RULE_PATTERN_INVALID);
        }
        if (AgentConstants.ESCALATION_TRIGGER_REGEX.equals(triggerType)) {
            try {
                Pattern.compile(rule.getPattern());
            } catch (PatternSyntaxException ex) {
                throw exception(SENSITIVE_RULE_PATTERN_INVALID);
            }
        }
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static AgentSensitiveRuleOptionsRespVO.Option option(String value, String label, String description) {
        return new AgentSensitiveRuleOptionsRespVO.Option(value, label, description);
    }

}
