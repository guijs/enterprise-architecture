package com.ea.framework.security;

import com.ea.common.enums.ResultCode;
import com.ea.common.exception.AuthException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static LoginUser getLoginUser() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new AuthException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static LoginUser getLoginUserOrNull() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static String getUsername() {
        return getLoginUser().getUsername();
    }
}
