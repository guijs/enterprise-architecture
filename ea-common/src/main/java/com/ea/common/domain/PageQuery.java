package com.ea.common.domain;

import com.ea.common.constant.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询参数
 */
@Data
@Schema(description = "分页查询参数")
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1")
    private Integer pageNum = Constants.DEFAULT_PAGE_NUM;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 200, message = "每页条数最大为 200")
    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = Constants.DEFAULT_PAGE_SIZE;

    @Schema(description = "排序字段")
    private String orderBy;

    @Schema(description = "排序方向：asc / desc", example = "desc")
    private String orderDirection = Constants.DESC;

    public int safePageNum() {
        return pageNum == null || pageNum < 1 ? Constants.DEFAULT_PAGE_NUM : pageNum;
    }

    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return Constants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, 200);
    }
}
