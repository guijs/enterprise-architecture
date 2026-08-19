package com.company.log.support;

import java.util.Set;

/**
 * 逐项日志决策结果。
 */
public record LogDecision(boolean enabled, boolean logRequest, boolean logResponse, Set<String> ignoreParams) {
}
