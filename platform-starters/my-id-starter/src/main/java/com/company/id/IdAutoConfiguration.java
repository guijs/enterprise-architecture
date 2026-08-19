package com.company.id;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 分布式 ID 自动装配：WorkerId 分配 + Snowflake 生成器 + IdHelper。
 * 需开启定时任务用于 WorkerId 续约（此处自带 @EnableScheduling）。
 */
@AutoConfiguration
@EnableScheduling
public class IdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WorkerIdAssigner workerIdAssigner(StringRedisTemplate redisTemplate, Environment environment) {
        return new WorkerIdAssigner(redisTemplate, environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public SnowflakeIdGenerator snowflakeIdGenerator(WorkerIdAssigner assigner) {
        return new SnowflakeIdGenerator(assigner.getWorkerId());
    }

    @Bean
    @ConditionalOnMissingBean
    public IdHelper idHelper(SnowflakeIdGenerator generator) {
        return new IdHelper(generator);
    }

    /**
     * MyBatis-Plus 集成：实体使用 @TableId(type = IdType.ASSIGN_ID) 自动走 Snowflake。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(IdentifierGenerator.class)
    static class MybatisPlusIdConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public IdentifierGenerator identifierGenerator(SnowflakeIdGenerator generator) {
            return entity -> generator.nextId();
        }
    }
}
