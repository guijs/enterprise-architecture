package com.company.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置：兼容 Spring Boot 4.1（DataRedisProperties）。
 * redisson-spring-boot-starter 3.37.0 的 auto-config 绑定 Boot 3 RedisProperties，
 * 在 Boot 4.1 中会失败，故此处提供自定义配置。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        var singleServerConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(database);
        if (password != null && !password.isBlank()) {
            singleServerConfig.setPassword(password);
        }
        return Redisson.create(config);
    }
}
