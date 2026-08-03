package com.ea.framework.security;

/**
 * 登录用户缓存抽象
 */
public interface LoginUserCache {

    void put(String token, LoginUser loginUser, long expireSeconds);

    LoginUser get(String token);

    void remove(String token);
}
