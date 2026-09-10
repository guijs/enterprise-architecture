package com.company.biz.service.service;

import com.company.biz.service.entity.OrderEntity;
import com.company.common.page.PageQuery;
import com.company.common.page.PageResult;

public interface OrderService {

    OrderEntity getById(Long id);

    Long create(OrderEntity order);

    PageResult<OrderEntity> page(PageQuery query);
}
