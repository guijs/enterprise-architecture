package com.ea.framework.security;

import com.ea.framework.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String createToken(Long userId, String username) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.getExpireSeconds() * 1000);
        return Jwts.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expire)
                .signWith(secretKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Object userId = parseClaims(token).get("userId");
        if (userId instanceof Integer integer) {
            return integer.longValue();
        }
        if (userId instanceof Long longValue) {
            return longValue;
        }
        return userId == null ? null : Long.valueOf(userId.toString());
    }

    public String resolveToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith(jwtProperties.getPrefix())) {
            return bearerToken.substring(jwtProperties.getPrefix().length()).trim();
        }
        return null;
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
