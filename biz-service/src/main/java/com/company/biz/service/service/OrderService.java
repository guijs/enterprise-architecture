package com.company.biz.service.service;

import com.company.biz.service.entity.OrderEntity;

public interface OrderService {

    OrderEntity getById(Long id);

    Long create(OrderEntity order);
}
