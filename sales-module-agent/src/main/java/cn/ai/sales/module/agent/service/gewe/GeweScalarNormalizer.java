package cn.ai.sales.module.agent.service.gewe;

import cn.ai.sales.framework.common.util.json.JsonUtils;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GeweScalarNormalizer {

    private static final String SCALAR_KEYS =
            "string|str|text|value|long|int|integer|number|bool|boolean|float|double";
    private static final String[] SCALAR_KEY_ARRAY = {
            "string", "str", "text", "value", "long", "int",
            "integer", "number", "bool", "boolean", "float", "double"
    };
    private static final Pattern MAP_STYLE_SCALAR =
            Pattern.compile("^\\{(?:" + SCALAR_KEYS + ")=([\\s\\S]*)}$");

    private GeweScalarNormalizer() {
    }

    public static String toWechatString(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof Map<?, ?> || unwrapped instanceof Collection<?>) {
            return null;
        }
        return Convert.toStr(unwrapped, null);
    }

    public static Object unwrap(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (String key : SCALAR_KEY_ARRAY) {
                if (map.containsKey(key)) {
                    return unwrap(map.get(key));
                }
            }
            if (map.size() == 1) {
                Optional<?> onlyValue = map.values().stream().findFirst();
                if (onlyValue.isPresent()) {
                    return unwrap(onlyValue.get());
                }
            }
            return value;
        }
        if (value instanceof Collection<?> collection && collection.size() == 1) {
            Optional<?> onlyValue = collection.stream().findFirst();
            if (onlyValue.isPresent()) {
                return unwrap(onlyValue.get());
            }
        }
        if (value instanceof CharSequence text) {
            return cleanText(text.toString());
        }
        return value;
    }

    public static String cleanText(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        String result = value.trim();
        for (int i = 0; i < 3; i++) {
            Matcher mapMatcher = MAP_STYLE_SCALAR.matcher(result);
            if (mapMatcher.matches()) {
                result = mapMatcher.group(1).trim();
                continue;
            }
            Map<String, Object> jsonObject = JsonUtils.parseObjectQuietly(result, new TypeReference<Map<String, Object>>() {
            });
            Object jsonScalar = firstScalarValue(jsonObject);
            if (jsonScalar != null) {
                result = Convert.toStr(jsonScalar, "").trim();
                continue;
            }
            break;
        }
        return unescapeJsonControlChars(result);
    }

    public static boolean isRawWechatIdentifier(String value) {
        String text = cleanText(value);
        return StrUtil.isNotBlank(text)
                && (StrUtil.startWith(text, "wxid_")
                || StrUtil.startWith(text, "gh_")
                || StrUtil.equals(text, "weixin")
                || StrUtil.endWith(text, "@chatroom"));
    }

    private static String unescapeJsonControlChars(String value) {
        if (value == null || !value.contains("\\")) {
            return value;
        }
        return value.replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static Object firstScalarValue(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        for (String key : SCALAR_KEY_ARRAY) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

}
