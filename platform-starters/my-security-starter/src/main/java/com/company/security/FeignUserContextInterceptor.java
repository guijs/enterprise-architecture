package com.company.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Feign 请求拦截器：将当前 UserContext 中的用户信息透传到下游服务。
 * 自动装配条件：classpath 存在 Feign。
 */
public class FeignUserContextInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String userId = UserContext.getUserId();
        String userName = UserContext.getUserName();
        if (userId != null) {
            template.header(UserInterceptor.HEADER_USER_ID, userId);
        }
        if (userName != null) {
            template.header(UserInterceptor.HEADER_USER_NAME, userName);
        }
    }
}
