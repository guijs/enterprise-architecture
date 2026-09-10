package com.company.lock;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 分布式锁切面：解析 SpEL key，Redisson tryLock，finally 释放。
 */
@Aspect
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
        String lockKey = buildKey(pjp, distributedLock);
        RLock lock = distributedLock.fair()
                ? redissonClient.getFairLock(lockKey)
                : redissonClient.getLock(lockKey);

        boolean acquired = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
        if (!acquired) {
            throw distributedLock.exception().getConstructor(String.class).newInstance(distributedLock.message());
        }
        try {
            return pjp.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String buildKey(ProceedingJoinPoint pjp, DistributedLock lock) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] paramNames = sig.getParameterNames();
        Object[] args = pjp.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        String keySuffix = Arrays.stream(lock.keys())
                .map(spel -> parser.parseExpression(spel).getValue(context, String.class))
                .collect(Collectors.joining(":"));
        String prefix = StrUtil.isBlank(lock.prefix())
                ? sig.getDeclaringType().getSimpleName() + ":" + sig.getName()
                : lock.prefix();
        return "lock:" + prefix + ":" + keySuffix;
    }
}
