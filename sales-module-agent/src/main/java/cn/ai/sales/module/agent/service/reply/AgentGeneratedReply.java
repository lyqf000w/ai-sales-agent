package cn.ai.sales.module.agent.service.reply;

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;

public record AgentGeneratedReply(String content,
                                  String matchedKnowledgeTitle,
                                  BigDecimal confidence,
                                  String generationSource,
                                  String llmProvider,
                                  String llmModel) {

    public static final String SOURCE_KNOWLEDGE = "KNOWLEDGE";
    public static final String SOURCE_DEEPSEEK = "DEEPSEEK";
    public static final String SOURCE_NONE = "NONE";

    public AgentGeneratedReply {
        confidence = confidence == null ? BigDecimal.ZERO : confidence;
        generationSource = StrUtil.blankToDefault(generationSource, SOURCE_NONE);
    }

    public AgentGeneratedReply(String content, String matchedKnowledgeTitle) {
        this(content, matchedKnowledgeTitle, new BigDecimal("0.80"));
    }

    public AgentGeneratedReply(String content, String matchedKnowledgeTitle, BigDecimal confidence) {
        this(content, matchedKnowledgeTitle, confidence, SOURCE_KNOWLEDGE, null, null);
    }

    public boolean hasContent() {
        return StrUtil.isNotBlank(content);
    }

    public static AgentGeneratedReply knowledge(String content, String matchedKnowledgeTitle) {
        return new AgentGeneratedReply(content, matchedKnowledgeTitle, new BigDecimal("0.80"),
                SOURCE_KNOWLEDGE, null, null);
    }

    public static AgentGeneratedReply llm(String content, String matchedKnowledgeTitle, BigDecimal confidence,
                                          String llmProvider, String llmModel) {
        return new AgentGeneratedReply(content, matchedKnowledgeTitle, confidence, SOURCE_DEEPSEEK,
                llmProvider, llmModel);
    }

    public static AgentGeneratedReply blank() {
        return new AgentGeneratedReply("", null, BigDecimal.ZERO, SOURCE_NONE, null, null);
    }

}
