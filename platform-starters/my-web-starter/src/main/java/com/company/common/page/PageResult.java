package com.company.common.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页出参：禁止各服务自定义 {list, count} 字段名。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    private List<T> records;
    private long total;
    private long pageNum;
    private long pageSize;

    /** 由 MyBatis-Plus IPage 转换（引入方存在 mybatis-plus 时可用）。 */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public long getPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (total + pageSize - 1) / pageSize;
    }
}
