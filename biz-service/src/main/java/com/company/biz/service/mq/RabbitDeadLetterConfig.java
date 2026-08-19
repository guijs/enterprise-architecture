package com.company.biz.service.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 死信队列配置：业务队列绑定死信交换机，消费失败（业务异常）转入死信。
 */
@Configuration(proxyBeanMethods = false)
public class RabbitDeadLetterConfig {

    public static final String ORDER_QUEUE = "order.queue";
    public static final String DLX_EXCHANGE = "dlx.exchange";
    public static final String DLX_ROUTING_KEY = "dlx.order";
    public static final String DLX_QUEUE = "dlx.order.queue";

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue dlxOrderQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxOrderQueue()).to(dlxExchange()).with(DLX_ROUTING_KEY);
    }
}
