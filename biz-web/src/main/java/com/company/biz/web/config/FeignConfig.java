package com.company.biz.web.config;

import com.company.security.UserInterceptor;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 配置：将用户上下文 Header 透传到下游服务。
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor userHeaderForwardInterceptor() {
        return template -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String userId = request.getHeader(UserInterceptor.HEADER_USER_ID);
                String userName = request.getHeader(UserInterceptor.HEADER_USER_NAME);
                if (userId != null) {
                    template.header(UserInterceptor.HEADER_USER_ID, userId);
                }
                if (userName != null) {
                    template.header(UserInterceptor.HEADER_USER_NAME, userName);
                }
            }
        };
    }
}
