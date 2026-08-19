package com.company.crypto;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 字段加密自动装配：启动时把密钥/盐注入静态 CryptoHelper，供 TypeHandler 使用。
 */
@AutoConfiguration
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoAutoConfiguration {

    public CryptoAutoConfiguration(CryptoProperties properties) {
        CryptoHelper.init(properties.getSecretKey(), properties.getHashSalt());
    }
}
