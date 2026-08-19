package com.company.common.exception;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 错误信息占位符填充：
 * - 命名占位 {key}（推荐）：与 Map 参数、前端 data 三方对齐
 * - 顺序占位 {}（类 SLF4J）：按出现顺序消费 args
 */
public final class ErrorMessageFormatter {

    private static final Pattern NAMED = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    private ErrorMessageFormatter() {
    }

    /** 命名占位：{skuId} → params.get("skuId")；缺 key 则保留原占位。 */
    public static String format(String template, Map<String, ?> params) {
        if (template == null || params == null || params.isEmpty()) {
            return template;
        }
        Matcher m = NAMED.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Object val = params.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(val == null ? m.group(0) : String.valueOf(val)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 顺序占位：与 SLF4J 类似，按出现顺序消费 args。 */
    public static String format(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template.length() + 16);
        int argIdx = 0;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '{' && i + 1 < template.length() && template.charAt(i + 1) == '}') {
                Object val = argIdx < args.length ? args[argIdx++] : "{}";
                sb.append(val);
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
