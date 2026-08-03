package com.ea.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 当前用户信息
 */
@Data
@Builder
@Schema(description = "用户信息")
public class UserInfoVO {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "权限标识集合")
    private Set<String> permissions;

    @Schema(description = "角色标识集合")
    private Set<String> roles;
}
