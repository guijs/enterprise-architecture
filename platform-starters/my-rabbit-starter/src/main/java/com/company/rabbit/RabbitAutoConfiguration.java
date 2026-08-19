package com.company.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * RabbitMQ 可靠性自动装配。
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RabbitReliableConfig rabbitReliableConfig() {
        return new RabbitReliableConfig();
    }
}
