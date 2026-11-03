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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Read-write separation routing aspect.
 * <p>
 * Handles @Master, @Slave annotations and auto-detects transaction type.
 * <p>
 * Higher priority than @Transactional to ensure routing is set before transaction starts.
 * <p>
 * <b>Transaction boundary:</b> a transaction binds exactly one connection when it
 * starts, and every statement inside it reuses that connection. The routing decision
 * is therefore taken once, at the outermost transactional method. A {@code @Slave}
 * call nested inside a transaction that already started on master (or the other way
 * round) cannot switch datasources — the aspect logs a warning instead of pretending
 * that it did.
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

    @Pointcut("@within(org.springframework.transaction.annotation.Transactional)")
    public void transactionalClassPointcut() {}

    @Around("masterPointcut() || masterClassPointcut()")
    public Object aroundMaster(ProceedingJoinPoint joinPoint) throws Throwable {
        Master master = getAnnotation(joinPoint, Master.class);
        String reason = master != null ? master.reason() : "";

        if (hasSlaveBoundTransaction()) {
            log.warn("[RW-Routing] @Master on {}.{} runs inside a transaction already bound to SLAVE; "
                            + "writes will hit the read replica. Declare @Master on the outermost transactional method.",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName());
        }

        log.debug("[RW-Routing] @Master intercepted: {}.{}, reason: {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                reason);

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER, null, true);
        try {
            Object result = joinPoint.proceed();
            // 命令方法成功后写入时间戳，使同一线程随后的读操作在
            // read-master-after-write 窗口内仍走主库（read-your-writes）。
            // aroundTransactional 在该场景下会因 current() != AUTO 提前返回，
            // 标记只能由本切面负责。
            ReadWriteRoutingContext.markWrite();
            return result;
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    @Around("slavePointcut() || slaveClassPointcut()")
    public Object aroundSlave(ProceedingJoinPoint joinPoint) throws Throwable {
        Slave slave = getAnnotation(joinPoint, Slave.class);

        String slaveName = slave != null ? slave.value() : "";
        boolean fallbackToMaster = slave == null || slave.fallbackToMaster();

        if (hasMasterBoundTransaction()) {
            log.warn("[RW-Routing] @Slave on {}.{} runs inside a transaction already bound to MASTER; "
                            + "the annotation has no effect inside that transaction.",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName());
        }

        log.debug("[RW-Routing] @Slave intercepted: {}.{}, slave: {}, fallbackToMaster: {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                slaveName.isEmpty() ? "auto" : slaveName,
                fallbackToMaster);

        ReadWriteRoutingContext.push(
                ReadWriteRoutingContext.RoutingType.SLAVE,
                slaveName.isEmpty() ? null : slaveName,
                fallbackToMaster);
        try {
            return joinPoint.proceed();
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    @Around("transactionalPointcut() || transactionalClassPointcut()")
    public Object aroundTransactional(ProceedingJoinPoint joinPoint) throws Throwable {
        Transactional transactional = getAnnotation(joinPoint, Transactional.class);
        ReadWriteRoutingContext.RoutingType current = ReadWriteRoutingContext.current();
        String targetType = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        if (current == ReadWriteRoutingContext.RoutingType.MASTER) {
            return joinPoint.proceed();
        }

        if (transactional != null && transactional.readOnly()) {
            if (current == ReadWriteRoutingContext.RoutingType.SLAVE) {
                return joinPoint.proceed();
            }

            log.debug("[RW-Routing] @Transactional(readOnly=true) intercepted: {}.{}", targetType, methodName);

            ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, null, true);
            try {
                return joinPoint.proceed();
            } finally {
                ReadWriteRoutingContext.pop();
            }
        }

        if (current == ReadWriteRoutingContext.RoutingType.SLAVE) {
            // A write transaction must never inherit a @Slave scope: the derived
            // connection would point at the read replica. Force master for this
            // transaction (it is too late only if an outer transaction is already
            // bound, which we flag).
            log.warn("[RW-Routing] Non-read-only @Transactional {}.{} is nested in a @Slave scope; "
                            + "forcing MASTER. Move @Master to the outermost transactional method.",
                    targetType, methodName);
        }

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER, null, true);
        try {
            Object result = joinPoint.proceed();
            ReadWriteRoutingContext.markWrite();
            return result;
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    private boolean hasSlaveBoundTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                && ReadWriteRoutingContext.current() == ReadWriteRoutingContext.RoutingType.SLAVE;
    }

    private boolean hasMasterBoundTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                && ReadWriteRoutingContext.current() == ReadWriteRoutingContext.RoutingType.MASTER;
    }

    private <T extends Annotation> T getAnnotation(ProceedingJoinPoint joinPoint, Class<T> annotationClass) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        T annotation = method.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }

        annotation = joinPoint.getTarget().getClass().getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }

        // Fall back to the interface declaration for JDK-proxy targets.
        Method targetMethod = findTargetMethod(joinPoint, signature);
        if (targetMethod != null) {
            annotation = targetMethod.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
            Class<?> declaringClass = targetMethod.getDeclaringClass();
            return declaringClass.getAnnotation(annotationClass);
        }

        return null;
    }

    private Method findTargetMethod(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        try {
            return joinPoint.getTarget().getClass()
                    .getMethod(signature.getName(), signature.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
