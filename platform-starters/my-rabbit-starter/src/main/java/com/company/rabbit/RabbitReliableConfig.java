package com.company.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RabbitMQ 可靠性回调装配：
 * - ConfirmCallback：Broker 是否收到消息（配合 publisher-confirm-type=correlated）
 * - ReturnsCallback：路由失败回退（配合 publisher-returns=true、mandatory=true）
 */
@Slf4j
public class RabbitReliableConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    @Autowired
    public void configure(RabbitTemplate rabbitTemplate) {
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (!ack) {
            log.error("消息投递到 Broker 失败, correlation={}, cause={}", correlationData, cause);
        }
    }

    @Override
    public void returnedMessage(org.springframework.amqp.core.ReturnedMessage returned) {
        log.error("消息路由失败, exchange={}, routingKey={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
    }
}
