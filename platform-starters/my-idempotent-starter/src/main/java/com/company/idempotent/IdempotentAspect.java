package com.company.idempotent;

import cn.hutool.core.util.StrUtil;
import com.company.common.exception.BizException;
import com.company.common.exception.CommonErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 幂等切面：两态。首个请求写 PENDING；成功后按需回写 DONE 结果；失败删除 key 允许重试。
 */
@Aspect
@RequiredArgsConstructor
public class IdempotentAspect {

    private static final String PENDING = "PENDING";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        String key = buildKey(pjp, idempotent);
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, PENDING, idempotent.expire(), idempotent.timeUnit());

        if (Boolean.FALSE.equals(acquired)) {
            String cached = redisTemplate.opsForValue().get(key);
            if (idempotent.cacheResult() && cached != null && !PENDING.equals(cached)) {
                Class<?> returnType = ((MethodSignature) pjp.getSignature()).getReturnType();
                return objectMapper.readValue(cached, returnType);
            }
            throw new BizException(CommonErrorCode.BAD_REQUEST.getCode(), idempotent.message());
        }

        try {
            Object result = pjp.proceed();
            if (idempotent.cacheResult() && result != null) {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result),
                        idempotent.resultExpire(), TimeUnit.SECONDS);
            } else {
                redisTemplate.delete(key);
            }
            return result;
        } catch (Exception e) {
            redisTemplate.delete(key);
            throw e;
        }
    }

    private String buildKey(ProceedingJoinPoint pjp, Idempotent idempotent) {
        if (StrUtil.isNotBlank(idempotent.key())) {
            return "idempotent:" + resolveSpel(pjp, idempotent.key());
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String token = attrs == null ? null : attrs.getRequest().getHeader("Idempotent-Token");
        if (StrUtil.isBlank(token)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST.getCode(), "幂等 Token 不能为空");
        }
        return "idempotent:" + token;
    }

    private String resolveSpel(ProceedingJoinPoint pjp, String spel) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] names = sig.getParameterNames();
        Object[] args = pjp.getArgs();
        EvaluationContext context = new StandardEvaluationContext();
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                context.setVariable(names[i], args[i]);
            }
        }
        return parser.parseExpression(spel).getValue(context, String.class);
    }
}
