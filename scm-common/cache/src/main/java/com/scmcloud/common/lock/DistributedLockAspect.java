package com.scmcloud.common.lock;

import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.exception.ErrorCode;
import com.scmcloud.common.redis.script.RedisLuaScriptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.locks.LockSupport;
import java.util.UUID;

/**
 * AOP aspect for @DistributedLock annotation.
 * Uses Redis Lua scripts for atomic lock acquisition and safe release.
 *
 * @author SCM Platform
 * @since 2026-06-04
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final StringRedisTemplate redisTemplate;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    private static final String LOCK_PREFIX = "lock:";

    private static final RedisScript<Long> ACQUIRE_SCRIPT =
            RedisLuaScriptLoader.load("lua/lock/acquire.lua", Long.class);

    private static final RedisScript<Long> UNLOCK_SCRIPT =
            RedisLuaScriptLoader.load("lua/lock/unlock.lua", Long.class);

    @Around("@annotation(com.scmcloud.common.lock.DistributedLockAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLockAnnotation annotation = method.getAnnotation(DistributedLockAnnotation.class);

        // Parse lock key from SpEL
        String lockKey = parseKey(annotation.key(), method, joinPoint.getArgs());
        String redisKey = LOCK_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString();

        // Try to acquire lock using Lua script (atomic check + set)
        long ttlMillis = annotation.unit().toMillis(annotation.ttl());
        Long result = tryAcquire(redisKey, lockValue, ttlMillis);

        if (result == null || result != 1L) {
            if (annotation.blocking()) {
                result = waitAndRetry(redisKey, lockValue, ttlMillis, annotation);
            }
        }

        if (result == null || result != 1L) {
            log.warn("Failed to acquire distributed lock: key={}, method={}", lockKey, method.getName());
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED, annotation.errorMessage());
        }

        try {
            log.debug("Acquired distributed lock: key={}", lockKey);
            return joinPoint.proceed();
        } finally {
            // Release lock using Lua script (atomic check owner + delete)
            redisTemplate.execute(
                    UNLOCK_SCRIPT,
                    Collections.singletonList(redisKey),
                    lockValue
            );
            log.debug("Released distributed lock: key={}", lockKey);
        }
    }

    private String parseKey(String keyExpression, Method method, Object[] args) {
        try {
            EvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = nameDiscoverer.getParameterNames(method);
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            return parser.parseExpression(keyExpression).getValue(context, String.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED,
                    "Invalid lock key expression: " + keyExpression);
        }
    }

    private Long tryAcquire(String redisKey, String lockValue, long ttlMillis) {
        return redisTemplate.execute(
                ACQUIRE_SCRIPT,
                Collections.singletonList(redisKey),
                lockValue,
                String.valueOf(ttlMillis)
        );
    }

    private Long waitAndRetry(String redisKey, String lockValue, long ttlMillis, DistributedLockAnnotation annotation) {
        long waitMillis = annotation.unit().toMillis(annotation.waitTime());
        long deadline = System.currentTimeMillis() + waitMillis;
        long delay = 10;

        while (System.currentTimeMillis() < deadline) {
            LockSupport.parkNanos(delay * 1_000_000L);
            if (Thread.interrupted()) {
                return null;
            }
            Long result = tryAcquire(redisKey, lockValue, ttlMillis);
            if (result != null && result == 1L) {
                return result;
            }
            delay = Math.clamp(delay * 2, 10, 200);
        }
        return null;
    }
}
