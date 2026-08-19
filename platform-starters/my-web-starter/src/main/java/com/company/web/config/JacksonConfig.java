package com.company.web.config;

import com.company.common.enums.BaseEnum;
import com.company.common.enums.BaseEnumDeserializer;
import com.company.common.enums.BaseEnumSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Jackson 全局序列化：
 * - Long / BigDecimal → String，规避前端 JS 精度丢失
 * - 统一日期格式
 * - 注册 BaseEnum 的 code/desc 序列化与反序列化
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
            builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");

            SimpleModule enumModule = new SimpleModule();
            enumModule.addSerializer(BaseEnum.class, new BaseEnumSerializer());
            enumModule.addDeserializer(BaseEnum.class, new BaseEnumDeserializer());
            builder.modulesToInstall(enumModule);
        };
    }
}
