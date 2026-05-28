package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentKnowledgeItemDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.ai.sales.module.agent.service.knowledge.AgentKnowledgeHit;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AgentKnowledgeReplyGenerator implements AgentReplyGenerator {

    @Autowired(required = false)
    private AgentLlmClient llmClient;

    @Override
    public AgentGeneratedReply generate(AgentReplyContext context) {
        if (llmClient != null && llmClient.supports(context.llmProvider(), context.llmModel())
                && llmClient.isEnabled()) {
            AgentGeneratedReply llmReply = llmClient.generate(context);
            if (llmReply.hasContent()) {
                return polish(llmReply);
            }
        }
        AgentGeneratedReply reply = generateHits(context.inboundContent(), context.knowledgeHits());
        if (reply.hasContent() || context.knowledgeHits() == null || context.knowledgeHits().isEmpty()) {
            return polish(reply);
        }
        AgentKnowledgeHit first = context.knowledgeHits().get(0);
        return polish(AgentGeneratedReply.knowledge(first.content(), first.title()));
    }

    public AgentGeneratedReply generate(String customerMessage, List<AgentKnowledgeItemDO> knowledgeItems) {
        if (StrUtil.isBlank(customerMessage) || knowledgeItems == null || knowledgeItems.isEmpty()) {
            return AgentGeneratedReply.blank();
        }
        return knowledgeItems.stream()
                .filter(item -> item != null && AgentConstants.STATUS_ENABLE == nullToZero(item.getStatus()))
                .filter(item -> StrUtil.isNotBlank(item.getAnswer()))
                .filter(item -> matches(customerMessage, item))
                .max(Comparator.comparing((AgentKnowledgeItemDO item) -> nullToZero(item.getSort()))
                        .thenComparing(item -> item.getId() == null ? 0L : item.getId()))
                .map(item -> polish(AgentGeneratedReply.knowledge(item.getAnswer(), item.getTitle())))
                .orElseGet(AgentGeneratedReply::blank);
    }

    private AgentGeneratedReply generateHits(String customerMessage, List<AgentKnowledgeHit> knowledgeHits) {
        if (StrUtil.isBlank(customerMessage) || knowledgeHits == null || knowledgeHits.isEmpty()) {
            return AgentGeneratedReply.blank();
        }
        return knowledgeHits.stream()
                .filter(hit -> hit != null && hit.hasContent())
                .filter(hit -> matches(customerMessage, hit))
                .max(Comparator.comparing((AgentKnowledgeHit hit) -> nullToZero(hit.keywordScore()))
                        .thenComparing(hit -> nullToZero(hit.sort()))
                        .thenComparing(hit -> hit.knowledgeChunkId() == null ? 0L : hit.knowledgeChunkId()))
                .map(hit -> polish(AgentGeneratedReply.knowledge(hit.content(), hit.title())))
                .orElseGet(AgentGeneratedReply::blank);
    }

    private AgentGeneratedReply polish(AgentGeneratedReply reply) {
        if (reply == null || !reply.hasContent()) {
            return reply == null ? AgentGeneratedReply.blank() : reply;
        }
        String polishedContent = AgentReplyTonePolisher.polish(reply.content());
        if (StrUtil.equals(polishedContent, reply.content())) {
            return reply;
        }
        return new AgentGeneratedReply(polishedContent, reply.matchedKnowledgeTitle(), reply.confidence(),
                reply.generationSource(), reply.llmProvider(), reply.llmModel());
    }

    private boolean matches(String customerMessage, AgentKnowledgeItemDO item) {
        if (StrUtil.isNotBlank(item.getKeywords())) {
            for (String keyword : item.getKeywords().split("[,，;；\\s]+")) {
                if (StrUtil.isNotBlank(keyword) && StrUtil.containsIgnoreCase(customerMessage, keyword.trim())) {
                    return true;
                }
            }
        }
        return StrUtil.isNotBlank(item.getQuestion()) && StrUtil.containsIgnoreCase(customerMessage, item.getQuestion());
    }

    private boolean matches(String customerMessage, AgentKnowledgeHit hit) {
        if (hit.keywordScore() != null && hit.keywordScore() > 0) {
            return true;
        }
        if (StrUtil.isNotBlank(hit.keywords())) {
            for (String keyword : hit.keywords().split("[,，;；\\s]+")) {
                if (StrUtil.isNotBlank(keyword) && StrUtil.containsIgnoreCase(customerMessage, keyword.trim())) {
                    return true;
                }
            }
        }
        return StrUtil.isNotBlank(hit.question()) && StrUtil.containsIgnoreCase(customerMessage, hit.question());
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

}
