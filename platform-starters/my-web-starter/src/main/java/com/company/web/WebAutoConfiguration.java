package com.company.web;

import com.company.common.exception.GlobalExceptionHandler;
import com.company.web.async.AsyncConfig;
import com.company.web.config.JacksonConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * my-web-starter 主自动装配：统一响应/异常、序列化、异步线程池。
 * 不使用 @ComponentScan，通过 @Import + @Bean 显式注册。
 */
@AutoConfiguration
@Import({JacksonConfig.class, AsyncConfig.class})
public class WebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
