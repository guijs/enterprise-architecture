package com.company.web.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Feign 自动装配：仅当引入方存在 OpenFeign 时生效。
 */
@AutoConfiguration
@ConditionalOnClass(ErrorDecoder.class)
public class FeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new FeignErrorDecoder(objectMapper);
    }
}
