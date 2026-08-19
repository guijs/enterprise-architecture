package com.company.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS 白名单配置：生产必须配置明确域名，禁止通配（allowedOriginPattern=* 与 credentials 不可同时使用）。
 */
@Data
@ConfigurationProperties(prefix = "gateway.cors")
public class GatewayCorsProperties {

    private List<String> allowedOrigins = List.of();
}
