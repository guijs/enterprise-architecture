package com.company.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Feign 请求拦截器：向下游服务 /internal/** 接口发送内部调用 Token。
 * 
 * 配置说明：
 * - security.internal.token: 内部调用共享密钥，必须与下游服务配置一致
 * - security.internal.header-name: Token Header 名称，默认 X-Internal-Token
 * 
 * 环境变量：INTERNAL_TOKEN
 */
public class FeignInternalTokenInterceptor implements RequestInterceptor {

    private final InternalSecurityProperties properties;

    public FeignInternalTokenInterceptor(InternalSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void apply(RequestTemplate template) {
        String path = template.path();
        if (isInternalPath(path)) {
            template.header(properties.headerName(), properties.token());
        }
    }

    private boolean isInternalPath(String path) {
        return path != null && (path.contains("/internal/") || path.endsWith("/internal"));
    }
}
