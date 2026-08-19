package com.company.security;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Web 层拦截器：从网关注入的 Header 解析用户信息并建立上下文。
 * 请求结束在 afterCompletion 清理，防止线程复用污染。
 */
public class UserInterceptor implements HandlerInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(HEADER_USER_ID);
        String userName = request.getHeader(HEADER_USER_NAME);
        if (StrUtil.isNotBlank(userId)) {
            String decodedName = StrUtil.isBlank(userName)
                    ? null
                    : URLDecoder.decode(userName, StandardCharsets.UTF_8);
            UserContext.set(new UserInfo(userId, decodedName));
            MDC.put("userId", userId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        UserContext.remove();
        MDC.remove("userId");
    }
}
