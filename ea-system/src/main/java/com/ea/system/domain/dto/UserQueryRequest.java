package com.ea.system.domain.dto;

import com.ea.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户查询参数")
public class UserQueryRequest extends PageQuery {

    @Schema(description = "用户名（模糊）")
    private String username;

    @Schema(description = "昵称（模糊）")
    private String nickname;

    @Schema(description = "状态：0 正常，1 停用")
    private Integer status;
}
