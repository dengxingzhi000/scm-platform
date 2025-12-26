package com.frog.common.seata.aspect;

import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 分布式事务切面
 *
 * <p>自动记录分布式事务的开始、提交、回滚等关键节点。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
public class GlobalTransactionalAspect {

    /**
     * 环绕通知：记录分布式事务执行情况
     */
    @Around("@annotation(io.seata.spring.annotation.GlobalTransactional)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        // 获取全局事务 XID
        String xid = RootContext.getXID();
        boolean isTransactionInitiator = (xid == null);

        if (isTransactionInitiator) {
            log.info("🌐 [Seata] 开始全局事务: {}", methodName);
        } else {
            log.info("🔗 [Seata] 加入全局事务: {}, XID: {}", methodName, xid);
        }

        long startTime = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();

            xid = RootContext.getXID();
            long duration = System.currentTimeMillis() - startTime;

            if (isTransactionInitiator) {
                log.info("✅ [Seata] 全局事务提交成功: {}, XID: {}, 耗时: {}ms",
                    methodName, xid, duration);
            } else {
                log.info("✅ [Seata] 分支事务提交成功: {}, XID: {}, 耗时: {}ms",
                    methodName, xid, duration);
            }

            return result;
        } catch (Throwable e) {
            xid = RootContext.getXID();
            long duration = System.currentTimeMillis() - startTime;

            if (isTransactionInitiator) {
                log.error("❌ [Seata] 全局事务回滚: {}, XID: {}, 耗时: {}ms, 原因: {}",
                    methodName, xid, duration, e.getMessage());
            } else {
                log.error("❌ [Seata] 分支事务回滚: {}, XID: {}, 耗时: {}ms, 原因: {}",
                    methodName, xid, duration, e.getMessage());
            }

            throw e;
        }
    }
}