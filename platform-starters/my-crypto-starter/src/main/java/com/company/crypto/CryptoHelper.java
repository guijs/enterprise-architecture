package com.company.crypto;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 字段加解密工具（AES-256-GCM）+ HMAC 等值查询哈希。
 * 供 TypeHandler 静态调用，密钥在启动时由自动配置注入。
 */
public final class CryptoHelper {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static volatile SecretKeySpec keySpec;
    private static volatile HMac hmac;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CryptoHelper() {
    }

    static void init(String secretKey, String hashSalt) {
        if (StrUtil.isNotBlank(secretKey)) {
            byte[] key = normalizeKey(secretKey);
            keySpec = new SecretKeySpec(key, "AES");
        }
        if (StrUtil.isNotBlank(hashSalt)) {
            hmac = new HMac(HmacAlgorithm.HmacSHA256, hashSalt.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static String encrypt(String plain) {
        if (plain == null || keySpec == null) {
            return plain;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("字段加密失败", e);
        }
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null || keySpec == null) {
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] data = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("字段解密失败", e);
        }
    }

    /** 不可逆哈希，用于密文列的等值查询。 */
    public static String hmac(String plain) {
        if (plain == null || hmac == null) {
            return null;
        }
        return hmac.digestHex(plain);
    }

    private static byte[] normalizeKey(String secretKey) {
        byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
        if (raw.length == 32) {
            return raw;
        }
        // 兼容 16 进制/短密钥：截断或右填充到 32 字节
        byte[] key = new byte[32];
        byte[] source = raw.length == 64 ? HexUtil.decodeHex(secretKey) : raw;
        System.arraycopy(source, 0, key, 0, Math.min(source.length, 32));
        return key;
    }
}
