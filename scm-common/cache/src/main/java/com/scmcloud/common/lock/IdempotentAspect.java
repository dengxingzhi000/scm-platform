package com.scmcloud.common.lock;

import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * AOP aspect for @Idempotent annotation.
 * Uses Redis SETNX to ensure idempotent execution.
 *
 * @author SCM Platform
 * @since 2026-06-04
 */
@Slf4j
@Aspect
@Component("lockIdempotentAspect")
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StringRedisTemplate redisTemplate;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";

    @Around("@annotation(com.scmcloud.common.lock.Idempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Idempotent annotation = method.getAnnotation(Idempotent.class);

        // Parse idempotency key from SpEL
        String idempotentKey = parseKey(annotation.key(), method, joinPoint.getArgs());
        String redisKey = IDEMPOTENT_KEY_PREFIX + idempotentKey;

        // Check if this is a duplicate request
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", annotation.ttl(), annotation.unit());
        if (Boolean.FALSE.equals(isNew)) {
            log.warn("Duplicate request detected: key={}, method={}", idempotentKey, method.getName());
            throw new BusinessException(ErrorCode.IDEMPOTENT_REPLAY, annotation.errorMessage());
        }

        try {
            log.debug("Executing idempotent operation: key={}", idempotentKey);
            Object result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            // Remove the idempotency key on failure to allow retry
            redisTemplate.delete(redisKey);
            throw e;
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
            log.warn("Failed to parse idempotent key expression '{}': {}", keyExpression, e.getMessage());
            return keyExpression;
        }
    }
}
