package com.ea.system.controller;

import com.ea.common.annotation.Log;
import com.ea.common.domain.R;
import com.ea.common.domain.PageResult;
import com.ea.system.domain.dto.UserQueryRequest;
import com.ea.system.domain.dto.UserSaveRequest;
import com.ea.system.domain.entity.SysUser;
import com.ea.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @Operation(summary = "分页查询用户")
    @PreAuthorize("hasAuthority('system:user:list') or hasAuthority('*:*:*')")
    @GetMapping("/page")
    public R<PageResult<SysUser>> page(UserQueryRequest request) {
        return R.ok(sysUserService.pageUsers(request));
    }

    @Operation(summary = "查询用户详情")
    @PreAuthorize("hasAuthority('system:user:query') or hasAuthority('*:*:*')")
    @GetMapping("/{id}")
    public R<SysUser> detail(@PathVariable Long id) {
        return R.ok(sysUserService.getById(id));
    }

    @Operation(summary = "新增用户")
    @Log(title = "用户管理", businessType = 1)
    @PreAuthorize("hasAuthority('*:*:*')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody UserSaveRequest request) {
        return R.ok(sysUserService.createUser(request));
    }

    @Operation(summary = "修改用户")
    @Log(title = "用户管理", businessType = 2)
    @PreAuthorize("hasAuthority('*:*:*')")
    @PutMapping
    public R<Void> update(@Valid @RequestBody UserSaveRequest request) {
        sysUserService.updateUser(request);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @Log(title = "用户管理", businessType = 3)
    @PreAuthorize("hasAuthority('*:*:*')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return R.ok();
    }
}
