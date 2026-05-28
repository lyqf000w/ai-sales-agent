package cn.ai.sales.module.agent.service.reply;

import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Keeps generated replies closer to a real WeChat chat instead of a service-script template.
 */
public final class AgentReplyTonePolisher {

    private static final List<String> ROBOTIC_PREFIXES = List.of(
            "您好，感谢您的咨询。",
            "您好，感谢您的咨询！",
            "您好，感谢咨询。",
            "您好，感谢咨询！",
            "感谢您的咨询。",
            "感谢您的咨询！",
            "感谢咨询。",
            "感谢咨询！",
            "您好，",
            "您好。",
            "你好，",
            "你好。"
    );

    private static final List<String> ROBOTIC_SUFFIXES = List.of(
            "如有其他问题，请随时联系我们。",
            "如有其他问题，请随时联系我。",
            "如有其他问题，欢迎随时咨询。",
            "如您还有其他问题，请随时联系我们。",
            "请问还有什么可以帮您？",
            "请问还有什么可以帮您的吗？",
            "希望以上信息对您有所帮助。",
            "感谢您的理解与支持。"
    );

    private static final Pattern AI_IDENTITY_PATTERN = Pattern.compile(
            "我是(?:一个)?\\s*(?:AI|ai|人工智能|机器人|智能客服|销售助手)(?:\\s*销售助手)?[，,。！？!?\\s]*");
    private static final Pattern KNOWLEDGE_PREFIX_PATTERN = Pattern.compile(
            "(?:根据|依据)(?:知识库|资料|相关资料|现有信息|系统记录)(?:显示|来看|内容)?[，,:：\\s]*");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s　]+");
    private static final Pattern MULTI_PUNCTUATION_PATTERN = Pattern.compile("([。！？!?])\\1+");

    private AgentReplyTonePolisher() {
    }

    public static String polish(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }
        String reply = content.trim();
        reply = stripSurroundingQuotes(reply);
        reply = AI_IDENTITY_PATTERN.matcher(reply).replaceAll("");
        reply = KNOWLEDGE_PREFIX_PATTERN.matcher(reply).replaceAll("");
        reply = removeKnownPrefixes(reply);
        reply = removeKnownSuffixes(reply);
        reply = normalizeText(reply);
        return StrUtil.blankToDefault(reply, content.trim());
    }

    private static String stripSurroundingQuotes(String reply) {
        String result = reply;
        if ((result.startsWith("\"") && result.endsWith("\""))
                || (result.startsWith("'") && result.endsWith("'"))
                || (result.startsWith("“") && result.endsWith("”"))) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }

    private static String removeKnownPrefixes(String reply) {
        String result = reply.trim();
        boolean changed;
        do {
            changed = false;
            for (String prefix : ROBOTIC_PREFIXES) {
                if (result.startsWith(prefix) && result.length() > prefix.length()) {
                    result = result.substring(prefix.length()).trim();
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return result;
    }

    private static String removeKnownSuffixes(String reply) {
        String result = reply.trim();
        boolean changed;
        do {
            changed = false;
            for (String suffix : ROBOTIC_SUFFIXES) {
                if (result.endsWith(suffix) && result.length() > suffix.length()) {
                    result = result.substring(0, result.length() - suffix.length()).trim();
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return result;
    }

    private static String normalizeText(String reply) {
        String result = WHITESPACE_PATTERN.matcher(reply).replaceAll(" ").trim();
        result = result.replace(" ，", "，")
                .replace(" 。", "。")
                .replace(" ？", "？")
                .replace(" ！", "！")
                .replace(" ,", ",")
                .replace(" .", ".")
                .replace(" ?", "?")
                .replace(" !", "!");
        result = MULTI_PUNCTUATION_PATTERN.matcher(result).replaceAll("$1");
        return result.trim();
    }

}
