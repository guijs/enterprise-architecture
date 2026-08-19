package com.company.ratelimit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 限流自动装配。
 */
@AutoConfiguration
public class RateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(StringRedisTemplate redisTemplate) {
        return new RateLimitAspect(redisTemplate);
    }
}
