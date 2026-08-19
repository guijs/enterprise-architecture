package com.company.log.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 日志辅助：入参过滤、JSON 序列化、截断、客户端 IP 等。
 */
public final class LogUtils {

    private LogUtils() {
    }

    public static String truncate(String text, int maxLen) {
        if (text == null) {
            return "-";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...(truncated)";
    }

    public static String safeToJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return JSONUtil.toJsonStr(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    /** 过滤敏感入参：命中 ignore 字段名的参数以 *** 替换。 */
    public static Object filterArgs(ProceedingJoinPoint pjp, Set<String> ignoreParams) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] names = sig.getParameterNames();
        Object[] args = pjp.getArgs();
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String name = names != null && i < names.length ? names[i] : "arg" + i;
            if (ignoreParams.contains(name)) {
                map.put(name, "***");
            } else {
                map.put(name, args[i]);
            }
        }
        return map;
    }

    public static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "-";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        return StrUtil.isNotBlank(ip) ? ip : request.getRemoteAddr();
    }
}
