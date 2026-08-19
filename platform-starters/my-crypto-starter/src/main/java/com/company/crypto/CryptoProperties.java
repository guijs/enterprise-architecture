package com.company.crypto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 字段加密配置。密钥仅从环境变量 / KMS 注入，禁止写进仓库。
 */
@Data
@ConfigurationProperties(prefix = "my.crypto")
public class CryptoProperties {

    /** AES_GCM | SM4_GCM。 */
    private String algorithm = "AES_GCM";

    private String secretKey;

    /** 用于等值查询的 HMAC 盐（不可逆）。 */
    private String hashSalt;
}
