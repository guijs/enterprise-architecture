package com.company.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 内部安全自动装配：注册内部接口 Token 验证拦截器和配置验证器。
 * 仅在 Servlet Web 环境且 security.internal.enabled=true 时生效。
 * 
 * 安全要点：
 * - 非 local/dev/test profile 必须配置 INTERNAL_TOKEN，否则启动失败
 * - Token 比较使用常量时间算法防止时序攻击
 * 
 * 配置示例：
 * security:
 *   internal:
 *     enabled: true
 *     token: ${INTERNAL_TOKEN:}
 *     header-name: X-Internal-Token
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnProperty(name = "security.internal.enabled", havingValue = "true")
@EnableConfigurationProperties(InternalSecurityProperties.class)
public class InternalSecurityAutoConfiguration {

    @Bean
    public InternalSecurityValidator internalSecurityValidator(
            InternalSecurityProperties properties, Environment environment) {
        return new InternalSecurityValidator(properties, environment);
    }

    @Bean
    public InternalTokenInterceptor internalTokenInterceptor(InternalSecurityProperties properties) {
        return new InternalTokenInterceptor(properties);
    }

    @Bean
    public WebMvcConfigurer internalSecurityWebMvcConfigurer(InternalTokenInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor)
                        .addPathPatterns("/internal/**", "/api/internal/**");
            }
        };
    }
}
