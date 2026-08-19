package com.company.web.doc;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
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

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openAPI(DocProperties props, Environment env) {
        String appName = env.getProperty("spring.application.name", "API 文档");
        return new OpenAPI().info(new Info()
                .title(StrUtil.isBlank(props.getTitle()) ? appName : props.getTitle())
                .description(props.getDescription())
                .version(props.getVersion())
                .contact(new Contact().name(props.getContactName())));
    }

    @Bean
    @ConditionalOnMissingBean
    public GroupedOpenApi defaultApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .pathsToMatch("/api/**")
                .build();
    }
}
