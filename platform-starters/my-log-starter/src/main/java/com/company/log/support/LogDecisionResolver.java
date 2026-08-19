package com.company.log.support;

import com.company.log.annotation.LogSwitch;
import com.company.log.config.InvokeLogProperties;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 日志决策器（Controller / Feign 共用）：逐项按「方法 > 类 > 全局」解析。
 */
public final class LogDecisionResolver {

    private LogDecisionResolver() {
    }

    public static LogDecision resolve(LogSwitch methodEnabled, LogSwitch methodReq,
                                      LogSwitch methodResp, String[] methodIgnore,
                                      LogSwitch classEnabled, LogSwitch classReq,
                                      LogSwitch classResp, String[] classIgnore,
                                      InvokeLogProperties.ChannelLogProperties global) {
        boolean enabled = pick(methodEnabled, classEnabled, global.isEnabled());
        boolean req = pick(methodReq, classReq, global.isLogRequest());
        boolean resp = pick(methodResp, classResp, global.isLogResponse());

        Set<String> ignore = new LinkedHashSet<>(global.getIgnoreParams());
        if (classIgnore != null) {
            ignore.addAll(Arrays.asList(classIgnore));
        }
        if (methodIgnore != null) {
            ignore.addAll(Arrays.asList(methodIgnore));
        }
        return new LogDecision(enabled, req, resp, ignore);
    }

    private static boolean pick(LogSwitch method, LogSwitch type, boolean global) {
        if (method != null && method != LogSwitch.DEFAULT) {
            return method == LogSwitch.ON;
        }
        if (type != null && type != LogSwitch.DEFAULT) {
            return type == LogSwitch.ON;
        }
        return global;
    }
}
