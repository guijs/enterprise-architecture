package com.company.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.company.gateway.model.UserInfo;
import com.company.gateway.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * 全局鉴权过滤器：白名单放行；其余校验 Token 并注入下游用户 Header。
 * 安全要点：注入前先剥离客户端可能伪造的同名 Header；下游需内网隔离，不可被外部直接访问。
 */
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITE_LIST = List.of("/auth/login", "/auth/refresh", "/actuator/health");

    private final TokenService tokenService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (WHITE_LIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        UserInfo userInfo = tokenService.parseToken(token);
        if (userInfo == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (StrUtil.isBlank(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        String finalTraceId = traceId;

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Name");
                })
                .header("X-User-Id", userInfo.getUserId())
                .header("X-User-Name", URLEncoder.encode(userInfo.getUserName(), StandardCharsets.UTF_8))
                .header("X-Trace-Id", finalTraceId)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
