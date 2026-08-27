package com.company.gateway.service;

import com.company.gateway.config.JwtProperties;
import com.company.gateway.model.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * JWT Token 服务：HS256 签名生成与验证。
 * Access Token ~30min，本地校验签名与有效期，解析 userId/userName。
 * 生产环境必须配置 gateway.jwt.secret（env JWT_SECRET）为强随机密钥。
 */
@Slf4j
@Service
public class TokenService {

    private final SecretKey secretKey;
    private final long expireMinutes;

    public TokenService(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.expireMinutes = jwtProperties.accessTokenExpireMinutes();
    }

    /**
     * 生成 Access Token（HS256 JWT）。
     */
    public String generateToken(String userId, String userName) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("userId", userId)
                .claim("userName", userName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireMinutes, ChronoUnit.MINUTES)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析并校验 Token，返回用户信息；无效返回 null。
     */
    public UserInfo parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        if (jwt.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            String userId = claims.get("userId", String.class);
            String userName = claims.get("userName", String.class);
            if (userId == null) {
                return null;
            }
            return new UserInfo(userId, userName);
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.debug("JWT invalid: {}", e.getMessage());
            return null;
        }
    }
}
