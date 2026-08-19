package com.company.biz.service.search;

import lombok.Data;

@Data
public class OrderSearchReq {

    private String keyword;

    private Integer status;
}
