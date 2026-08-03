package com.ea.framework.security;

import com.ea.common.utils.ServletUtils;
import com.ea.framework.config.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Token 与登录态缓存服务
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final LoginUserCache loginUserCache;

    public String createLoginToken(LoginUser loginUser) {
        String token = jwtTokenProvider.createToken(loginUser.getUserId(), loginUser.getUsername());
        loginUser.setToken(token);
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + jwtProperties.getExpireSeconds() * 1000);
        loginUser.setIpaddr(ServletUtils.getClientIp());
        cacheLoginUser(loginUser);
        return token;
    }

    public void cacheLoginUser(LoginUser loginUser) {
        loginUserCache.put(loginUser.getToken(), loginUser, jwtProperties.getExpireSeconds());
    }

    public LoginUser getLoginUser(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return loginUserCache.get(token);
    }

    public void refreshToken(LoginUser loginUser) {
        long remain = loginUser.getExpireTime() - System.currentTimeMillis();
        if (remain <= jwtProperties.getRefreshThresholdSeconds() * 1000) {
            loginUser.setLoginTime(System.currentTimeMillis());
            loginUser.setExpireTime(loginUser.getLoginTime() + jwtProperties.getExpireSeconds() * 1000);
            cacheLoginUser(loginUser);
        }
    }

    public void removeToken(String token) {
        if (token != null && !token.isBlank()) {
            loginUserCache.remove(token);
        }
    }
}
