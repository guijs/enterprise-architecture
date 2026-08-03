package com.ea.framework.security;

import com.ea.framework.config.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("enterprise-architecture-jwt-secret-key-change-me-please-32bytes");
        properties.setExpireSeconds(3600);
        properties.setPrefix("Bearer ");
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    void createAndParseToken() {
        String token = jwtTokenProvider.createToken(1L, "admin");
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("admin", jwtTokenProvider.getUsername(token));
        assertEquals(1L, jwtTokenProvider.getUserId(token));
        assertEquals(token, jwtTokenProvider.resolveToken("Bearer " + token));
    }
}
