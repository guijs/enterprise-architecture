package com.company.web.doc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "springdoc")
public class DocProperties {

    private boolean enabled = true;

    /** 不配则默认读取 spring.application.name。 */
    private String title;

    private String description;

    private String version = "1.0.0";

    private String contactName;
}
