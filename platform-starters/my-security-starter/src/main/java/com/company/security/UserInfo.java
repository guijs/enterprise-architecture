package com.company.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户上下文承载对象，由网关解析 Token 后透传 Header，Web 层拦截器还原。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private String userId;

    private String userName;
}
