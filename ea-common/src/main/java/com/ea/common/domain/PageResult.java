package com.ea.common.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "当前页码")
    private long pageNum;

    @Schema(description = "每页条数")
    private long pageSize;

    @Schema(description = "总页数")
    private long pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords() == null ? Collections.emptyList() : page.getRecords());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(Collections.emptyList());
        result.setTotal(0);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setPages(0);
        return result;
    }
}
