package com.company.biz.service.search;

import lombok.Data;

/**
 * ES 文档模型（对应索引 order_search_v1，别名 order_search）。
 */
@Data
public class OrderDoc {

    private String orderId;
    private String title;
    private Integer status;
    private Double amount;
    private Long createTime;
}
