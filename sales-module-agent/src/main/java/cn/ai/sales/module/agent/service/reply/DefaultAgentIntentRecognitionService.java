package cn.ai.sales.module.agent.service.reply;

import cn.ai.sales.module.agent.dal.dataobject.AgentDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentMessageDO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatContactDO;
import cn.ai.sales.module.agent.enums.AgentConstants;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

@Service
public class DefaultAgentIntentRecognitionService implements AgentIntentRecognitionService {

    @Override
    public AgentIntentAnalysis recognize(AgentDO agent, AgentWechatContactDO contact, AgentMessageDO inboundMessage) {
        String content = inboundMessage == null ? "" : StrUtil.blankToDefault(inboundMessage.getContent(), "");
        if (StrUtil.isBlank(content)) {
            return AgentIntentAnalysis.none();
        }
        if (containsAny(content, "退款", "退费", "退钱", "赔偿", "投诉", "差评", "维权", "骗子")) {
            return new AgentIntentAnalysis("refund_or_complaint", resolveSentiment(content, true), true,
                    AgentConstants.RISK_LEVEL_RED, "0.88", "客户表达退款、投诉或强烈不满");
        }
        if (containsAny(content, "法务", "律师", "起诉", "监管", "合规", "医疗", "金融")) {
            return new AgentIntentAnalysis("legal_compliance", resolveSentiment(content, false), true,
                    AgentConstants.RISK_LEVEL_RED, "0.84", "客户涉及法务、合规或高风险行业话题");
        }
        if (containsAny(content, "人工", "真人", "负责人", "老板", "经理", "销售联系")) {
            return new AgentIntentAnalysis("human_request", resolveSentiment(content, false), true,
                    AgentConstants.RISK_LEVEL_YELLOW, "0.82", "客户明确要求人工介入");
        }
        if (containsAny(content, "知识库", "答不上", "答不出来", "找不到答案", "无法引用", "引用不了", "乱答")) {
            return new AgentIntentAnalysis("knowledge_gap", resolveSentiment(content, false), true,
                    AgentConstants.RISK_LEVEL_YELLOW, "0.80", "客户质疑知识库命中或专业回答可靠性");
        }
        if (containsAny(content, "大客户", "重点客户", "重点项目", "项目推进", "立项", "采购流程", "招采", "招标")) {
            return new AgentIntentAnalysis("key_customer_project", resolveSentiment(content, false), true,
                    AgentConstants.RISK_LEVEL_YELLOW, "0.78", "客户涉及重点客户或大客户项目推进");
        }
        if (containsAny(content, "合同", "承诺", "保证", "最低价", "独家", "赔付")) {
            return new AgentIntentAnalysis("special_commitment", resolveSentiment(content, false), true,
                    AgentConstants.RISK_LEVEL_YELLOW, "0.78", "客户涉及合同或特殊承诺");
        }
        if (containsAny(content, "报价", "价格", "费用", "多少钱", "怎么收费")) {
            return new AgentIntentAnalysis("price_consultation", "neutral", false,
                    AgentConstants.RISK_LEVEL_GREEN, "0.74", "客户咨询价格或费用");
        }
        return new AgentIntentAnalysis("general_consultation", resolveSentiment(content, false), false,
                AgentConstants.RISK_LEVEL_GREEN, "0.60", "普通销售咨询");
    }

    private String resolveSentiment(String content, boolean negativeByDefault) {
        if (containsAny(content, "生气", "愤怒", "气死", "垃圾", "差评", "投诉", "骗子", "不满意")) {
            return "angry";
        }
        return negativeByDefault ? "negative" : "neutral";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (StrUtil.containsIgnoreCase(text, keyword)) {
                return true;
            }
        }
        return false;
    }

}
