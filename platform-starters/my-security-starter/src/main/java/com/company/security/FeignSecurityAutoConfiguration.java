package com.company.security;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Feign 用户上下文自动装配：仅在 Servlet Web 环境且引入 Feign 时生效。
 * WebFlux 网关不会加载此配置，避免引入 servlet 依赖。
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class FeignSecurityAutoConfiguration {

    @Bean
    public FeignUserContextInterceptor feignUserContextInterceptor() {
        return new FeignUserContextInterceptor();
    }
}
