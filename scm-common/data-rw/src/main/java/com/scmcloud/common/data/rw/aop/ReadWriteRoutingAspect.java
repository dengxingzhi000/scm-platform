package com.scmcloud.common.data.rw.aop;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.data.rw.routing.ReadWriteRoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Read-write separation routing aspect.
 * <p>
 * Handles @Master, @Slave annotations and auto-detects transaction type.
 * <p>
 * Higher priority than @Transactional to ensure routing is set before transaction starts.
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReadWriteRoutingAspect {

    @Pointcut("@annotation(com.scmcloud.common.data.rw.annotation.Master)")
    public void masterPointcut() {}

    @Pointcut("@annotation(com.scmcloud.common.data.rw.annotation.Slave)")
    public void slavePointcut() {}

    @Pointcut("@within(com.scmcloud.common.data.rw.annotation.Master)")
    public void masterClassPointcut() {}

    @Pointcut("@within(com.scmcloud.common.data.rw.annotation.Slave)")
    public void slaveClassPointcut() {}

    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalPointcut() {}

    @Around("masterPointcut() || masterClassPointcut()")
    public Object aroundMaster(ProceedingJoinPoint joinPoint) throws Throwable {
        Master master = getAnnotation(joinPoint, Master.class);
        String reason = master != null ? master.reason() : "";

        log.debug("[RW-Routing] @Master intercepted: {}.{}, reason: {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                reason);

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        try {
            return joinPoint.proceed();
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    @Around("slavePointcut() || slaveClassPointcut()")
    public Object aroundSlave(ProceedingJoinPoint joinPoint) throws Throwable {
        Slave slave = getAnnotation(joinPoint, Slave.class);

        String slaveName = slave != null ? slave.value() : "";
        if (!slaveName.isEmpty()) {
            ReadWriteRoutingContext.specifySlave(slaveName);
        }

        log.debug("[RW-Routing] @Slave intercepted: {}.{}, slave: {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                slaveName.isEmpty() ? "auto" : slaveName);

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
        try {
            return joinPoint.proceed();
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    @Around("transactionalPointcut()")
    public Object aroundTransactional(ProceedingJoinPoint joinPoint) throws Throwable {
        Transactional transactional = getAnnotation(joinPoint, Transactional.class);

        if (ReadWriteRoutingContext.current() != ReadWriteRoutingContext.RoutingType.AUTO) {
            return joinPoint.proceed();
        }

        if (transactional != null && transactional.readOnly()) {
            log.debug("[RW-Routing] @Transactional(readOnly=true) intercepted: {}.{}",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName());

            ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
            try {
                return joinPoint.proceed();
            } finally {
                ReadWriteRoutingContext.pop();
            }
        } else {
            ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
            try {
                Object result = joinPoint.proceed();
                ReadWriteRoutingContext.markWrite();
                return result;
            } finally {
                ReadWriteRoutingContext.pop();
            }
        }
    }

    private <T extends Annotation> T getAnnotation(ProceedingJoinPoint joinPoint, Class<T> annotationClass) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        T annotation = method.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }

        return joinPoint.getTarget().getClass().getAnnotation(annotationClass);
    }
}
