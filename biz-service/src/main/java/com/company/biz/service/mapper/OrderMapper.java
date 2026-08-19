package com.company.biz.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.biz.service.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
}
