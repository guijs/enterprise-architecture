package com.company.gateway.service;

import com.company.gateway.model.UserInfo;
import org.springframework.stereotype.Service;

/**
 * Token 解析服务（骨架）。生产实现：用认证中心公钥校验 JWT 签名与有效期，解析 userId/userName。
 * 见 6.31：Access Token(JWT) 网关本地校验签名，Refresh Token 存 Redis 可撤销。
 */
@Service
public class TokenService {

    /**
     * 解析并校验 Token，返回用户信息；无效返回 null。
     * TODO 接入认证中心公钥校验 RS256 JWT。
     */
    public UserInfo parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        // 骨架占位：此处应校验 JWT 签名与过期时间，并从 claims 读取 userId / userName
        return null;
    }
}
