package com.company.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.common.page.PageResult;

/**
 * PageResult 与 MyBatis-Plus IPage 的转换工具。
 * 仅在 my-mybatis-starter 中提供，避免 my-web-starter 硬依赖 mybatis-plus。
 */
public final class PageResults {

    private PageResults() {
    }

    /**
     * 将 MyBatis-Plus IPage 转换为统一分页出参。
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
