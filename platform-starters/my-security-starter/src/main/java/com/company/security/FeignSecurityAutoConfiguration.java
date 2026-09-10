package com.company.security;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Feign 用户上下文自动装配：仅在 Servlet Web 环境且引入 Feign 时生效。
 * WebFlux 网关不会加载此配置，避免引入 servlet 依赖。
 * 
 * 包含两个拦截器：
 * 1. FeignUserContextInterceptor: 透传用户上下文（X-User-Id, X-User-Name, X-Trace-Id）
 * 2. FeignInternalTokenInterceptor: 向 /internal/** 接口发送内部调用 Token
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(InternalSecurityProperties.class)
public class FeignSecurityAutoConfiguration {

    @Bean
    public FeignUserContextInterceptor feignUserContextInterceptor() {
        return new FeignUserContextInterceptor();
    }

    @Bean
    public FeignInternalTokenInterceptor feignInternalTokenInterceptor(InternalSecurityProperties properties) {
        return new FeignInternalTokenInterceptor(properties);
    }
}
