package com.company.security;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 自动装配：注册用户上下文拦截器（仅 Servlet Web 环境生效）；
 * 当 Feign 在 classpath 时，同时注册 Feign 请求拦截器透传用户信息。
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
public class SecurityAutoConfiguration {

    @Bean
    public UserInterceptor userInterceptor() {
        return new UserInterceptor();
    }

    @Bean
    public WebMvcConfigurer userContextWebMvcConfigurer(UserInterceptor userInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(userInterceptor).addPathPatterns("/**");
            }
        };
    }

    @Bean
    @ConditionalOnClass(RequestInterceptor.class)
    public FeignUserContextInterceptor feignUserContextInterceptor() {
        return new FeignUserContextInterceptor();
    }
}
