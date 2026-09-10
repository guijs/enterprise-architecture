package com.company.biz.service.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 自定义业务指标。注意控制 tag 基数，禁止把订单号/userId 等高基数值当 tag。
 * 仅在 MeterRegistry 可用时装配（需要 actuator/micrometer 依赖）。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnBean(MeterRegistry.class)
public class OrderMetrics {

    private final MeterRegistry registry;

    public void countCreated(String channel) {
        registry.counter("biz.order.created", "channel", channel).increment();
    }

    public <T> T recordCreateTimer(Supplier<T> action) {
        return registry.timer("biz.order.create.timer").record(action);
    }
}
