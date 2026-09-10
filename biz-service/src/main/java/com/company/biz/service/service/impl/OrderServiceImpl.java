package com.company.biz.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.biz.service.entity.OrderEntity;
import com.company.biz.service.enums.OrderStatus;
import com.company.biz.service.mapper.OrderMapper;
import com.company.biz.service.metrics.OrderMetrics;
import com.company.biz.service.exception.OrderErrorCode;
import com.company.biz.service.service.OrderService;
import com.company.common.exception.BizException;
import com.company.common.exception.CommonErrorCode;
import com.company.common.page.PageQuery;
import com.company.common.page.PageResult;
import com.company.lock.DistributedLock;
import com.company.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现：演示分布式锁、业务异常、指标埋点、归属校验。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderMetrics orderMetrics;

    @Override
    public OrderEntity getById(Long id) {
        Long currentUserId = requireCurrentUserId();

        OrderEntity entity = orderMapper.selectById(id);
        if (entity == null) {
            throw new BizException(OrderErrorCode.ORDER_NOT_FOUND);
        }

        if (!currentUserId.equals(entity.getBuyerId())) {
            throw new BizException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        return entity;
    }

    @Override
    @DistributedLock(keys = {"#order.orderNo"}, prefix = "order:create", message = "订单正在处理中，请勿重复提交")
    public Long create(OrderEntity order) {
        Long currentUserId = requireCurrentUserId();
        String currentUserName = UserContext.getUserName();

        order.setBuyerId(currentUserId);
        order.setBuyerName(currentUserName);
        order.setStatus(OrderStatus.PENDING);
        orderMapper.insert(order);
        orderMetrics.countCreated("api");
        return order.getId();
    }

    @Override
    public PageResult<OrderEntity> page(PageQuery query) {
        Long currentUserId = requireCurrentUserId();

        LambdaQueryWrapper<OrderEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderEntity::getBuyerId, currentUserId);
        wrapper.orderByDesc(OrderEntity::getCreateTime);

        Page<OrderEntity> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<OrderEntity> result = orderMapper.selectPage(page, wrapper);

        return PageResult.of(result);
    }

    private Long requireCurrentUserId() {
        String userIdStr = UserContext.getUserId();
        if (StrUtil.isBlank(userIdStr)) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
    }
}
