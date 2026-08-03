package com.ea.framework.aspect;

import com.ea.common.annotation.Log;
import com.ea.common.utils.JsonUtils;
import com.ea.common.utils.ServletUtils;
import com.ea.framework.security.LoginUser;
import com.ea.framework.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Around("@annotation(controllerLog)")
    public Object around(ProceedingJoinPoint joinPoint, Log controllerLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            record(joinPoint, controllerLog, result, error, System.currentTimeMillis() - start);
        }
    }

    private void record(ProceedingJoinPoint joinPoint, Log controllerLog, Object result, Throwable error, long cost) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        HttpServletRequest request = ServletUtils.getRequest();
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();

        String username = loginUser == null ? "anonymous" : loginUser.getUsername();
        String uri = request == null ? "-" : request.getRequestURI();
        String method = request == null ? "-" : request.getMethod();
        String ip = ServletUtils.getClientIp();

        String requestData = controllerLog.saveRequestData()
                ? JsonUtils.toJson(joinPoint.getArgs())
                : "-";
        String responseData = controllerLog.saveResponseData()
                ? JsonUtils.toJson(result)
                : "-";

        if (error == null) {
            log.info("[操作日志] title={}, businessType={}, user={}, {} {}, ip={}, cost={}ms, args={}, result={}",
                    controllerLog.title(),
                    controllerLog.businessType(),
                    username,
                    method,
                    uri,
                    ip,
                    cost,
                    requestData,
                    responseData);
        } else {
            log.warn("[操作日志] title={}, businessType={}, user={}, {} {}, ip={}, cost={}ms, args={}, error={}",
                    controllerLog.title(),
                    controllerLog.businessType(),
                    username,
                    method,
                    uri,
                    ip,
                    cost,
                    requestData,
                    error.getMessage());
        }
    }
}
