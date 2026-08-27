package com.company.web.doc;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * SpringDoc + Knife4j 自动装配。生产可通过 springdoc.enabled=false 关闭文档。
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(prefix = "springdoc", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DocProperties.class)
public class DocAutoConfiguration {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_NAME = "X-User-Name";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openAPI(DocProperties props, Environment env) {
        String appName = env.getProperty("spring.application.name", "API 文档");
        return new OpenAPI()
                .info(new Info()
                        .title(StrUtil.isBlank(props.getTitle()) ? appName : props.getTitle())
                        .description(props.getDescription())
                        .version(props.getVersion())
                        .contact(new Contact().name(props.getContactName())))
                .components(new Components()
                        .addSecuritySchemes(HEADER_USER_ID, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(HEADER_USER_ID)
                                .description("用户 ID（如 1）"))
                        .addSecuritySchemes(HEADER_USER_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(HEADER_USER_NAME)
                                .description("用户名（如 admin）"))
                        .addSecuritySchemes(HEADER_TRACE_ID, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(HEADER_TRACE_ID)
                                .description("链路追踪 ID（可选）")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(HEADER_USER_ID)
                        .addList(HEADER_USER_NAME)
                        .addList(HEADER_TRACE_ID));
    }

    @Bean
    @ConditionalOnMissingBean
    public GroupedOpenApi defaultApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .pathsToMatch("/**")
                .build();
    }
}
