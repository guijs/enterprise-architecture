package com.company.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关侧用户信息（WebFlux 环境独立定义，不复用 servlet 的 my-security-starter）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private String userId;

    private String userName;
}
