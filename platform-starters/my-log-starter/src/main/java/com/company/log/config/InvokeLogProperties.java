package com.company.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 接口 / Feign 日志全局配置。方法/类注解可逐项覆盖。
 */
@Data
@ConfigurationProperties(prefix = "my.log")
public class InvokeLogProperties {

    private ChannelLogProperties controller = new ChannelLogProperties();
    private ChannelLogProperties feign = new ChannelLogProperties();
    private OperationProperties operation = new OperationProperties();

    @Data
    public static class ChannelLogProperties {
        private boolean enabled = true;
        private boolean logRequest = true;
        private boolean logResponse = false;
        private int maxBodyLength = 2048;
        private List<String> ignoreParams = List.of("password", "token");
        /** 仅 controller 使用。 */
        private List<String> excludePaths = List.of();
    }

    @Data
    public static class OperationProperties {
        private boolean enabled = true;
        private boolean async = true;
    }
}
