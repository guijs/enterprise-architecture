package com.ea.system.service;

import com.ea.common.enums.ResultCode;
import com.ea.common.exception.AuthException;
import com.ea.common.exception.BusinessException;
import com.ea.framework.config.properties.JwtProperties;
import com.ea.framework.security.LoginUser;
import com.ea.framework.security.SecurityUtils;
import com.ea.framework.security.TokenService;
import com.ea.system.domain.dto.LoginRequest;
import com.ea.system.domain.dto.RegisterRequest;
import com.ea.system.domain.entity.SysUser;
import com.ea.system.domain.vo.LoginVO;
import com.ea.system.domain.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    public LoginVO login(LoginRequest request) {
        SysUser user = sysUserService.getByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException("账号已停用，请联系管理员");
        }

        LoginUser loginUser = buildLoginUser(user);
        String token = tokenService.createLoginToken(loginUser);
        return LoginVO.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpireSeconds())
                .userInfo(toUserInfo(loginUser, user))
                .build();
    }

    public void register(RegisterRequest request) {
        if (sysUserService.getByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setStatus(0);
        user.setGender(0);
        sysUserService.save(user);
    }

    public void logout() {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser != null) {
            tokenService.removeToken(loginUser.getToken());
        }
    }

    public UserInfoVO currentUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = sysUserService.getById(loginUser.getUserId());
        if (user == null) {
            throw new AuthException(ResultCode.UNAUTHORIZED, "用户不存在或已被删除");
        }
        return toUserInfo(loginUser, user);
    }

    private LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setNickname(user.getNickname());
        loginUser.setStatus(user.getStatus());
        // 基础架构默认权限，后续可扩展角色/菜单体系
        if ("admin".equals(user.getUsername())) {
            loginUser.setRoles(Set.of("admin"));
            loginUser.setPermissions(Set.of("*:*:*"));
        } else {
            loginUser.setRoles(Set.of("user"));
            loginUser.setPermissions(Set.of("system:user:list", "system:user:query"));
        }
        return loginUser;
    }

    private UserInfoVO toUserInfo(LoginUser loginUser, SysUser user) {
        return UserInfoVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .permissions(loginUser.getPermissions())
                .roles(loginUser.getRoles())
                .build();
    }
}
