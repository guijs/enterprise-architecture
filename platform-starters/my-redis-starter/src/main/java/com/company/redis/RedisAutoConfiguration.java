package com.company.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 规范化自动装配：RedisTemplate 序列化、key 工具、缓存模板。
 */
@AutoConfiguration
@Import(RedisConfig.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisKeyHelper redisKeyHelper() {
        return new RedisKeyHelper();
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean
    public CacheTemplate cacheTemplate(StringRedisTemplate redisTemplate,
                                       RedissonClient redissonClient,
                                       ObjectMapper objectMapper) {
        return new CacheTemplate(redisTemplate, redissonClient, objectMapper);
    }
}
