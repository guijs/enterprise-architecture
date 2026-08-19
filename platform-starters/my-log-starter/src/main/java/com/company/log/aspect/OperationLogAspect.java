package com.company.log.aspect;

import cn.hutool.core.util.StrUtil;
import com.company.common.response.TraceContext;
import com.company.log.annotation.OperationLog;
import com.company.log.operation.OperationLogEntity;
import com.company.log.operation.OperationLogService;
import com.company.log.support.LogUtils;
import com.company.security.UserContext;
import com.company.security.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 操作审计切面：环绕 @OperationLog，记录结果/耗时后异步落库。
 */
@Aspect
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLogEntity entity = buildBase(pjp, operationLog);
        try {
            Object result = pjp.proceed();
            entity.setStatus(1);
            if (operationLog.saveResult()) {
                entity.setResult(LogUtils.truncate(LogUtils.safeToJson(result), 2048));
            }
            return result;
        } catch (Throwable t) {
            entity.setStatus(0);
            entity.setErrorMsg(StrUtil.sub(t.getMessage(), 0, 500));
            throw t;
        } finally {
            entity.setCostMs(System.currentTimeMillis() - start);
            entity.setContent(resolveSpel(pjp, operationLog.content()));
            operationLogService.saveAsync(entity);
        }
    }

    private OperationLogEntity buildBase(ProceedingJoinPoint pjp, OperationLog op) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        OperationLogEntity entity = new OperationLogEntity();
        entity.setModule(op.module());
        entity.setType(op.type());
        entity.setMethod(sig.getDeclaringType().getSimpleName() + "#" + sig.getName());
        entity.setTraceId(TraceContext.traceId());
        entity.setCreateTime(LocalDateTime.now());
        UserInfo user = UserContext.get();
        if (user != null) {
            entity.setOperatorId(user.getUserId());
            entity.setOperatorName(user.getUserName());
        }
        if (op.saveParams()) {
            entity.setParams(LogUtils.truncate(LogUtils.safeToJson(pjp.getArgs()), 2048));
        }
        HttpServletRequest request = LogUtils.currentRequest();
        if (request != null) {
            entity.setRequestUri(request.getRequestURI());
            entity.setRequestMethod(request.getMethod());
            entity.setIp(LogUtils.getClientIp(request));
        }
        return entity;
    }

    private String resolveSpel(ProceedingJoinPoint pjp, String expr) {
        if (StrUtil.isBlank(expr)) {
            return "";
        }
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        EvaluationContext ctx = new MethodBasedEvaluationContext(null, method, pjp.getArgs(), nameDiscoverer);
        return Optional.ofNullable(parser.parseExpression(expr).getValue(ctx, String.class)).orElse(expr);
    }
}
