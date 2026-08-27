package com.company.gateway.controller;

import com.company.gateway.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 认证接口（WebFlux）：网关本地处理登录，生成 JWT Access Token。
 * 本地演示用户：admin/admin123 (userId=1)。
 * 生产环境应接入认证中心或用户服务。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;

    /**
     * 登录接口，验证用户名密码后返回 JWT。
     * 本地演示只支持 admin/admin123。
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("code", 400, "message", "用户名和密码不能为空")));
        }

        if ("admin".equals(request.username()) && "admin123".equals(request.password())) {
            String token = tokenService.generateToken("1", "admin");
            return Mono.just(ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "success",
                    "data", Map.of(
                            "accessToken", token,
                            "tokenType", "Bearer",
                            "expiresIn", 1800
                    )
            )));
        }

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", 401, "message", "用户名或密码错误")));
    }

    public record LoginRequest(String username, String password) {}
}
