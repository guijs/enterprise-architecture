package com.company.biz.web.feign;

import lombok.Data;

/**
 * 订单分页查询参数（调用内部接口使用）。
 */
@Data
public class OrderPageQuery {

    private long pageNum = 1;

    private long pageSize = 20;

    private String orderBy;

    private String orderDir = "desc";
}
