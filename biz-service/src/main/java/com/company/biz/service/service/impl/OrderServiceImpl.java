package com.company.biz.service.service.impl;

import com.company.biz.service.entity.OrderEntity;
import com.company.biz.service.enums.OrderStatus;
import com.company.biz.service.mapper.OrderMapper;
import com.company.biz.service.metrics.OrderMetrics;
import com.company.biz.service.exception.OrderErrorCode;
import com.company.biz.service.service.OrderService;
import com.company.common.exception.BizException;
import com.company.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现（骨架）：演示分布式锁、业务异常、指标埋点。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderMetrics orderMetrics;

    @Override
    public OrderEntity getById(Long id) {
        OrderEntity entity = orderMapper.selectById(id);
        if (entity == null) {
            throw new BizException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        return entity;
    }

    @Override
    @DistributedLock(keys = {"#order.orderNo"}, prefix = "order:create", message = "订单正在处理中，请勿重复提交")
    public Long create(OrderEntity order) {
        order.setStatus(OrderStatus.PENDING);
        orderMapper.insert(order);
        orderMetrics.countCreated("api");
        return order.getId();
    }
}
