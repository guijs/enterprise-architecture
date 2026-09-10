package com.company.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Set;

/**
 * 内部安全配置验证器：确保非 local 环境已正确配置 INTERNAL_TOKEN。
 * 
 * 安全要点：
 * - local/dev profile 允许使用默认弱 token 方便开发调试
 * - 非 local 环境（如 prod）缺少 token 配置时启动失败（fail-fast）
 * - 防止生产环境意外使用弱默认 token
 */
public class InternalSecurityValidator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(InternalSecurityValidator.class);

    private static final Set<String> ALLOWED_WEAK_TOKEN_PROFILES = Set.of("local", "dev", "test");

    private final InternalSecurityProperties properties;
    private final Environment environment;

    public InternalSecurityValidator(InternalSecurityProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.enabled()) {
            log.info("Internal security is disabled, skipping token validation");
            return;
        }

        String[] activeProfiles = environment.getActiveProfiles();
        boolean isAllowedWeakTokenProfile = Arrays.stream(activeProfiles)
                .anyMatch(ALLOWED_WEAK_TOKEN_PROFILES::contains);

        if (properties.isTokenMissing()) {
            if (isAllowedWeakTokenProfile) {
                log.warn("INTERNAL_TOKEN not configured, using default weak token for profile: {}. " +
                         "This is acceptable for local/dev/test but MUST be configured in production.",
                         Arrays.toString(activeProfiles));
            } else {
                throw new IllegalStateException(
                        "INTERNAL_TOKEN must be configured for non-local profiles. " +
                        "Active profiles: " + Arrays.toString(activeProfiles) + ". " +
                        "Set environment variable INTERNAL_TOKEN or property security.internal.token");
            }
        } else if (properties.isUsingWeakDefaultToken() && !isAllowedWeakTokenProfile) {
            throw new IllegalStateException(
                    "Production environment must not use the default weak token 'local-internal-token-for-dev'. " +
                    "Active profiles: " + Arrays.toString(activeProfiles) + ". " +
                    "Set a strong INTERNAL_TOKEN via environment variable or K8s Secret.");
        }

        log.info("Internal security token validated for profiles: {}", Arrays.toString(activeProfiles));
    }
}
