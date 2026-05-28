package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;

public record AgentIntentAnalysis(String intent,
                                  String sentiment,
                                  boolean needsHuman,
                                  Integer riskLevel,
                                  BigDecimal confidence,
                                  String reason) {

    public AgentIntentAnalysis(String intent, String sentiment, boolean needsHuman, Integer riskLevel,
                               BigDecimal confidence, String reason) {
        this.intent = StrUtil.blankToDefault(intent, "unknown");
        this.sentiment = StrUtil.blankToDefault(sentiment, "neutral");
        this.needsHuman = needsHuman;
        this.riskLevel = riskLevel == null ? AgentConstants.RISK_LEVEL_GREEN : riskLevel;
        this.confidence = confidence == null ? BigDecimal.ZERO : confidence;
        this.reason = StrUtil.blankToDefault(reason, "未识别到明确升级意图");
    }

    public AgentIntentAnalysis(String intent, String sentiment, boolean needsHuman, Integer riskLevel,
                               String confidence, String reason) {
        this(intent, sentiment, needsHuman, riskLevel,
                NumberUtil.isNumber(confidence) ? new BigDecimal(confidence) : BigDecimal.ZERO, reason);
    }

    public static AgentIntentAnalysis none() {
        return new AgentIntentAnalysis("unknown", "neutral", false, AgentConstants.RISK_LEVEL_GREEN,
                BigDecimal.ZERO, "未识别到明确升级意图");
    }

}
