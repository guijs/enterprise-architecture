package com.company.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性。
 * 生产环境必须通过环境变量 JWT_SECRET 配置强密钥，不要使用默认值。
 */
@ConfigurationProperties(prefix = "gateway.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpireMinutes
) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            secret = "local-dev-jwt-secret-change-in-prod-must-be-at-least-32-chars";
        }
        if (accessTokenExpireMinutes <= 0) {
            accessTokenExpireMinutes = 30;
        }
    }
}
