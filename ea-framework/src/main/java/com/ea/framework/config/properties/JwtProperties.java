package com.ea.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ea.jwt")
public class JwtProperties {

    /** 签名密钥（至少 256 bit） */
    private String secret = "enterprise-architecture-jwt-secret-key-change-me-please-32bytes";

    /** 过期时间（秒），默认 2 小时 */
    private long expireSeconds = 7200;

    /** 刷新阈值（秒），剩余时间低于该值可刷新 */
    private long refreshThresholdSeconds = 1800;

    /** Token 请求头 */
    private String header = "Authorization";

    /** Token 前缀 */
    private String prefix = "Bearer ";
}
