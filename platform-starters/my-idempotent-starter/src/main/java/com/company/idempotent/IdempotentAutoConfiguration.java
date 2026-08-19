package com.company.idempotent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 幂等自动装配。
 */
@AutoConfiguration
public class IdempotentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new IdempotentAspect(redisTemplate, objectMapper);
    }
}
