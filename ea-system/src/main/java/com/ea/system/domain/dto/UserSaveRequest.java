package com.ea.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户新增/修改请求
 */
@Data
@Schema(description = "用户保存请求")
public class UserSaveRequest {

    @Schema(description = "用户 ID，更新时必填")
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 30, message = "用户名长度需在 3-30 之间")
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码，新增时必填")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别：0 未知，1 男，2 女")
    private Integer gender;

    @Schema(description = "状态：0 正常，1 停用")
    private Integer status;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "备注")
    private String remark;
}
