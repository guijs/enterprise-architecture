package com.company.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * JWT 认证白名单配置。
 * 
 * 白名单路径不需要 JWT 认证即可访问。
 * 
 * 安全要点：
 * - 生产环境禁止将 Swagger/API 文档路径加入白名单
 * - 本地/开发环境可通过配置启用 Swagger 白名单
 * 
 * 配置示例：
 * gateway:
 *   auth:
 *     whitelist:
 *       - /auth/login
 *       - /auth/refresh
 *       - /actuator/health
 */
@ConfigurationProperties(prefix = "gateway.auth")
public record AuthWhitelistProperties(
    List<String> whitelist
) {
    private static final List<String> DEFAULT_WHITELIST = List.of(
            "/auth/login",
            "/auth/refresh",
            "/actuator/health"
    );

    public AuthWhitelistProperties {
        if (whitelist == null || whitelist.isEmpty()) {
            whitelist = DEFAULT_WHITELIST;
        }
    }
}
