package com.company.common.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一分页入参：内置页大小硬上限，避免无界查询。
 */
@Data
public class PageQuery implements Serializable {

    @Min(1)
    private long pageNum = 1;

    @Min(1)
    @Max(100)
    private long pageSize = 20;

    /** 排序字段（业务侧走白名单，禁止直接拼 SQL）。 */
    private String orderBy;

    /** asc / desc。 */
    private String orderDir = "desc";

    public long offset() {
        return (pageNum - 1) * pageSize;
    }
}
