package com.company.log.aspect;

import com.company.common.response.TraceContext;
import com.company.log.annotation.LogSwitch;
import com.company.log.annotation.RequestLog;
import com.company.log.config.InvokeLogProperties;
import com.company.log.support.LogDecision;
import com.company.log.support.LogDecisionResolver;
import com.company.log.support.LogUtils;
import com.company.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Controller 接口日志切面：切 @RestController，按方法/类/全局逐项决策。
 */
@Slf4j
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@RequiredArgsConstructor
public class RequestLogAspect {

    private final InvokeLogProperties props;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Class<?> clazz = method.getDeclaringClass();
        HttpServletRequest request = LogUtils.currentRequest();

        if (request != null && pathExcluded(request.getRequestURI())) {
            return pjp.proceed();
        }

        RequestLog onMethod = method.getAnnotation(RequestLog.class);
        RequestLog onClass = clazz.getAnnotation(RequestLog.class);
        LogDecision d = LogDecisionResolver.resolve(
                ann(onMethod, RequestLog::enabled), ann(onMethod, RequestLog::request),
                ann(onMethod, RequestLog::response), onMethod == null ? null : onMethod.ignoreParams(),
                ann(onClass, RequestLog::enabled), ann(onClass, RequestLog::request),
                ann(onClass, RequestLog::response), onClass == null ? null : onClass.ignoreParams(),
                props.getController());

        if (!d.enabled()) {
            return pjp.proceed();
        }

        long start = System.currentTimeMillis();
        int maxLen = props.getController().getMaxBodyLength();
        String in = d.logRequest()
                ? LogUtils.truncate(LogUtils.safeToJson(LogUtils.filterArgs(pjp, d.ignoreParams())), maxLen)
                : "-";

        Object result = null;
        Throwable error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            String out = "-";
            if (d.logResponse() && error == null) {
                out = LogUtils.truncate(LogUtils.safeToJson(result), maxLen);
            }
            log.info("REQUEST_LOG traceId={} userId={} {} {} ip={} cost={}ms ok={} req={} resp={} err={}",
                    TraceContext.traceId(), UserContext.getUserId(),
                    request == null ? "-" : request.getMethod(),
                    request == null ? "-" : request.getRequestURI(),
                    LogUtils.getClientIp(request),
                    System.currentTimeMillis() - start, error == null, in, out,
                    error == null ? "" : error.getMessage());
        }
    }

    private boolean pathExcluded(String uri) {
        return props.getController().getExcludePaths().stream().anyMatch(p -> pathMatcher.match(p, uri));
    }

    private static LogSwitch ann(RequestLog a, Function<RequestLog, LogSwitch> f) {
        return a == null ? LogSwitch.DEFAULT : f.apply(a);
    }
}
