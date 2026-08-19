package com.company.biz.service.mq;

import com.company.common.exception.BizException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 消费者规范：手动 ACK。业务异常不重试进死信；系统异常 requeue 重试。
 * 消费端必须幂等（见 6.8，用 messageId / 业务唯一键去重）。
 */
@Slf4j
@Component
public class OrderConsumer {

    @RabbitListener(queues = RabbitDeadLetterConfig.ORDER_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            doProcess(message);
            channel.basicAck(deliveryTag, false);
        } catch (BizException e) {
            log.warn("消费业务异常，消息进死信队列", e);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("消费系统异常，消息重新入队", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private void doProcess(Message message) {
        // 骨架：解析消息、幂等校验、执行业务
        log.info("处理订单消息, size={}", message.getBody().length);
    }
}
