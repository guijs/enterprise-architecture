package com.ea.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全白名单配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ea.security")
public class SecurityProperties {

    /** 匿名可访问路径 */
    private List<String> permitAll = new ArrayList<>(List.of(
            "/auth/login",
            "/auth/register",
            "/api/ping",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/doc.html",
            "/webjars/**",
            "/error",
            "/h2-console/**"
    ));
}
