package com.company.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Feign 请求拦截器：将当前线程的用户上下文（UserContext）和追踪 ID 透传到下游服务。
 * 复制 X-User-Id、X-User-Name、X-Trace-Id 请求头，确保跨服务调用保持用户身份和追踪链。
 */
public class FeignUserContextInterceptor implements RequestInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String MDC_TRACE_KEY = "tid";

    @Override
    public void apply(RequestTemplate template) {
        UserInfo userInfo = UserContext.get();
        if (userInfo != null) {
            if (userInfo.getUserId() != null) {
                template.header(HEADER_USER_ID, userInfo.getUserId());
            }
            if (userInfo.getUserName() != null) {
                template.header(HEADER_USER_NAME, URLEncoder.encode(userInfo.getUserName(), StandardCharsets.UTF_8));
            }
        }
        String traceId = MDC.get(MDC_TRACE_KEY);
        if (traceId != null && !traceId.isBlank()) {
            template.header(HEADER_TRACE_ID, traceId);
        }
    }
}
