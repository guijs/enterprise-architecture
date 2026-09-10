package com.company.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内部安全配置属性。
 * 用于配置 /internal/** 路径的访问控制。
 * 
 * 环境变量说明：
 * - INTERNAL_TOKEN: 内部调用共享密钥，biz-web 和 biz-service 必须配置相同的值
 *   - 生产环境：必须通过 K8s Secret 注入强密钥
 *   - 本地环境：使用默认值 local-internal-token-for-dev 方便调试
 */
@ConfigurationProperties(prefix = "security.internal")
public record InternalSecurityProperties(
    boolean enabled,
    String token,
    String headerName
) {
    public static final String DEFAULT_HEADER_NAME = "X-Internal-Token";
    public static final String DEFAULT_LOCAL_TOKEN = "local-internal-token-for-dev";

    public InternalSecurityProperties {
        if (headerName == null || headerName.isBlank()) {
            headerName = DEFAULT_HEADER_NAME;
        }
        if (token == null || token.isBlank()) {
            token = DEFAULT_LOCAL_TOKEN;
        }
    }
}
