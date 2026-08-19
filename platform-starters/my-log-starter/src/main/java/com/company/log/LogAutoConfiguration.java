package com.company.log;

import com.company.log.aspect.FeignLogAspect;
import com.company.log.aspect.OperationLogAspect;
import com.company.log.aspect.RequestLogAspect;
import com.company.log.config.InvokeLogProperties;
import com.company.log.operation.LoggingOperationLogService;
import com.company.log.operation.OperationLogService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * my-log-starter 自动装配：注册接口/Feign 日志切面与审计切面。
 */
@AutoConfiguration
@EnableConfigurationProperties(InvokeLogProperties.class)
public class LogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OperationLogService operationLogService() {
        return new LoggingOperationLogService();
    }

    @Bean
    @ConditionalOnMissingBean
    public OperationLogAspect operationLogAspect(OperationLogService operationLogService) {
        return new OperationLogAspect(operationLogService);
    }

    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnMissingBean
    public RequestLogAspect requestLogAspect(InvokeLogProperties props) {
        return new RequestLogAspect(props);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignClient")
    @ConditionalOnMissingBean
    public FeignLogAspect feignLogAspect(InvokeLogProperties props) {
        return new FeignLogAspect(props);
    }
}
