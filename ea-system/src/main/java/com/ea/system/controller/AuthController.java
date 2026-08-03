package com.ea.system.controller;

import com.ea.common.annotation.Log;
import com.ea.common.domain.R;
import com.ea.system.domain.dto.LoginRequest;
import com.ea.system.domain.dto.RegisterRequest;
import com.ea.system.domain.vo.LoginVO;
import com.ea.system.domain.vo.UserInfoVO;
import com.ea.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @Log(title = "用户登录", businessType = 0, saveResponseData = false)
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @Operation(summary = "注册")
    @Log(title = "用户注册", businessType = 1)
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return R.ok();
    }

    @Operation(summary = "退出登录")
    @Log(title = "退出登录", businessType = 0)
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public R<UserInfoVO> me() {
        return R.ok(authService.currentUser());
    }
}
