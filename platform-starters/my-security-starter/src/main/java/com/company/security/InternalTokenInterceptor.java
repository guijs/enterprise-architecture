package com.company.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 内部接口安全拦截器：拦截 /internal/** 路径，验证 X-Internal-Token header。
 * 
 * 安全要点：
 * - 仅供内部服务（如 biz-web）通过 Feign 调用，不可被外部直接访问
 * - 必须配合 K8s NetworkPolicy 实现双重防护
 * - 没有有效 token 的请求返回 403 Forbidden
 */
public class InternalTokenInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(InternalTokenInterceptor.class);

    private final InternalSecurityProperties properties;

    public InternalTokenInterceptor(InternalSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        
        if (!isInternalPath(path)) {
            return true;
        }

        String token = request.getHeader(properties.headerName());
        
        if (token == null || token.isBlank()) {
            log.warn("Internal endpoint access denied: missing {} header, path={}, remoteAddr={}",
                    properties.headerName(), path, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"Access denied: internal endpoint requires authentication\"}");
            return false;
        }

        if (!properties.token().equals(token)) {
            log.warn("Internal endpoint access denied: invalid {} header, path={}, remoteAddr={}",
                    properties.headerName(), path, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"Access denied: invalid internal token\"}");
            return false;
        }

        return true;
    }

    private boolean isInternalPath(String path) {
        String contextPath = "";
        if (path.startsWith("/api")) {
            contextPath = "/api";
        }
        String relativePath = path.substring(contextPath.length());
        return relativePath.startsWith("/internal/") || relativePath.equals("/internal");
    }
}
