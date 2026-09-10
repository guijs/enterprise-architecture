package com.company.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Set;

/**
 * Feign 内部调用 Token 验证器：确保 Feign 客户端在非 local 环境已正确配置 INTERNAL_TOKEN。
 * 
 * 与 InternalSecurityValidator 的区别：
 * - InternalSecurityValidator 用于服务端（接收 /internal/** 请求时验证 token）
 * - FeignInternalTokenValidator 用于客户端（发起 Feign 调用 /internal/** 时携带 token）
 * 
 * 安全要点：
 * - local/dev/test profile 允许使用默认弱 token 方便开发调试
 * - 非 local 环境（如 prod）缺少 token 配置时启动失败（fail-fast）
 * - 即使服务端未启用 security.internal.enabled，客户端仍需配置 token 以便调用下游
 */
public class FeignInternalTokenValidator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(FeignInternalTokenValidator.class);

    private static final Set<String> ALLOWED_WEAK_TOKEN_PROFILES = Set.of("local", "dev", "test");

    private final InternalSecurityProperties properties;
    private final Environment environment;

    public FeignInternalTokenValidator(InternalSecurityProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isAllowedWeakTokenProfile = Arrays.stream(activeProfiles)
                .anyMatch(ALLOWED_WEAK_TOKEN_PROFILES::contains);

        if (properties.isTokenMissing()) {
            if (isAllowedWeakTokenProfile) {
                log.warn("INTERNAL_TOKEN not configured for Feign internal calls, using default weak token for profile: {}. " +
                         "This is acceptable for local/dev/test but MUST be configured in production.",
                         Arrays.toString(activeProfiles));
            } else {
                throw new IllegalStateException(
                        "INTERNAL_TOKEN must be configured for Feign internal API calls in non-local profiles. " +
                        "Active profiles: " + Arrays.toString(activeProfiles) + ". " +
                        "Set environment variable INTERNAL_TOKEN or property security.internal.token. " +
                        "This token is required for biz-web to call biz-service /internal/** endpoints.");
            }
        } else if (properties.isUsingWeakDefaultToken() && !isAllowedWeakTokenProfile) {
            throw new IllegalStateException(
                    "Production environment must not use the default weak token 'local-internal-token-for-dev' for Feign calls. " +
                    "Active profiles: " + Arrays.toString(activeProfiles) + ". " +
                    "Set a strong INTERNAL_TOKEN via environment variable or K8s Secret.");
        }

        log.info("Feign internal token validated for profiles: {}", Arrays.toString(activeProfiles));
    }
}
