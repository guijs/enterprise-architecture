package com.company.common.response;

import cn.hutool.core.util.StrUtil;
import org.slf4j.MDC;

/**
 * 链路上下文：读取 SkyWalking 注入的 tid（无 Agent 时可为空）。
 */
public final class TraceContext {

    private TraceContext() {
    }

    public static String traceId() {
        String tid = MDC.get("tid");
        return StrUtil.isBlank(tid) ? "" : tid;
    }
}
