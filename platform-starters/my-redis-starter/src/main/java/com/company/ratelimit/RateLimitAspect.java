package com.company.ratelimit;

import cn.hutool.core.util.StrUtil;
import com.company.common.exception.BizException;
import com.company.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 限流切面：滑动窗口 Lua 脚本，member 用「时间戳 + 唯一后缀」避免同毫秒并发覆盖。
 */
@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultRedisScript<Long> script = buildScript();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = buildKey(pjp, rateLimit);
        long now = System.currentTimeMillis();
        long windowMs = rateLimit.timeUnit().toMillis(rateLimit.window());
        String member = now + ":" + ThreadLocalRandom.current().nextLong();

        Long result = redisTemplate.execute(script, List.of(key),
                String.valueOf(now), String.valueOf(windowMs), String.valueOf(rateLimit.limit()), member);

        if (result == null || result == 0) {
            throw new BizException(CommonErrorCode.TOO_MANY_REQUESTS.getCode(),
                    rateLimit.message(), HttpStatus.TOO_MANY_REQUESTS);
        }
        return pjp.proceed();
    }

    private String buildKey(ProceedingJoinPoint pjp, RateLimit rateLimit) {
        if (StrUtil.isNotBlank(rateLimit.key())) {
            return "rate:" + resolveSpel(pjp, rateLimit.key());
        }
        String ip = clientIp();
        return "rate:" + ip + ":" + pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName();
    }

    private String clientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "-";
        }
        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        return request.getRemoteAddr();
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

    private static DefaultRedisScript<Long> buildScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/sliding_window.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
