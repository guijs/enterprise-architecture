package com.company.log.aspect;

import com.company.common.response.TraceContext;
import com.company.log.annotation.FeignLog;
import com.company.log.annotation.LogSwitch;
import com.company.log.config.InvokeLogProperties;
import com.company.log.support.LogDecision;
import com.company.log.support.LogDecisionResolver;
import com.company.log.support.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Feign 调用日志切面：切 @FeignClient 接口，按方法/接口/全局逐项决策。
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class FeignLogAspect {

    private final InvokeLogProperties props;

    @Around("execution(* *(..)) && @within(org.springframework.cloud.openfeign.FeignClient)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Class<?> feignType = resolveFeignType(pjp);
        Method ifaceMethod = feignType.getMethod(method.getName(), method.getParameterTypes());

        FeignLog onMethod = ifaceMethod.getAnnotation(FeignLog.class);
        FeignLog onClass = feignType.getAnnotation(FeignLog.class);
        LogDecision d = LogDecisionResolver.resolve(
                ann(onMethod, FeignLog::enabled), ann(onMethod, FeignLog::request),
                ann(onMethod, FeignLog::response), onMethod == null ? null : onMethod.ignoreParams(),
                ann(onClass, FeignLog::enabled), ann(onClass, FeignLog::request),
                ann(onClass, FeignLog::response), onClass == null ? null : onClass.ignoreParams(),
                props.getFeign());

        if (!d.enabled()) {
            return pjp.proceed();
        }

        String client = feignType.getSimpleName() + "#" + ifaceMethod.getName();
        long start = System.currentTimeMillis();
        int maxLen = props.getFeign().getMaxBodyLength();
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
            if (d.logResponse()) {
                out = error != null
                        ? ("EX:" + error.getClass().getSimpleName() + ":" + error.getMessage())
                        : LogUtils.truncate(LogUtils.safeToJson(result), maxLen);
            }
            log.info("FEIGN_LOG traceId={} client={} cost={}ms ok={} req={} resp={}",
                    TraceContext.traceId(), client, System.currentTimeMillis() - start, error == null, in, out);
        }
    }

    private Class<?> resolveFeignType(ProceedingJoinPoint pjp) {
        Class<?>[] interfaces = pjp.getTarget().getClass().getInterfaces();
        for (Class<?> itf : interfaces) {
            if (itf.isAnnotationPresent(org.springframework.cloud.openfeign.FeignClient.class)) {
                return itf;
            }
        }
        return pjp.getSignature().getDeclaringType();
    }

    private static LogSwitch ann(FeignLog a, Function<FeignLog, LogSwitch> f) {
        return a == null ? LogSwitch.DEFAULT : f.apply(a);
    }
}
