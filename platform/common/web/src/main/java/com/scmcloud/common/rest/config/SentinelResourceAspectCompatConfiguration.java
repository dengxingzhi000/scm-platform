package com.scmcloud.common.rest.config;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * Replaces Sentinel's own resource aspect with one that tolerates declarative HTTP clients.
 *
 * <p>{@code SentinelResourceAspect} looks up {@code @SentinelResource} on the most specific
 * implementation method. For an {@code @HttpExchange} interface the implementation is an
 * {@code HttpServiceInvocationHandler}, the annotation is never found, and every call died with
 * "Wrong state for SentinelResource annotation". Because {@code ApiAccessControlFilter} fails closed
 * on any lookup error, that single defect denied every business API in services that had Sentinel
 * enabled.
 *
 * <p>Registering this bean first satisfies the starter's {@code @ConditionalOnMissingBean}, keeps
 * annotation-driven limiting for ordinary classes, and lets interface clients fall through to
 * {@code HttpExchangeFallbackAspect}, which already resolves the annotation on the interface.
 */
@Configuration
public class SentinelResourceAspectCompatConfiguration {

    @Bean
    SentinelResourceAspect sentinelResourceAspect() {
        return new InterfaceTolerantSentinelResourceAspect();
    }

    @Aspect
    static class InterfaceTolerantSentinelResourceAspect extends SentinelResourceAspect {

        @Around("@annotation(com.alibaba.csp.sentinel.annotation.SentinelResource)")
        @Override
        public Object invokeResourceWithSentinel(ProceedingJoinPoint pjp) throws Throwable {
            Method signatureMethod = ((MethodSignature) pjp.getSignature()).getMethod();
            Object target = pjp.getTarget();
            Method targetMethod = AopUtils.getMostSpecificMethod(signatureMethod,
                    target == null ? signatureMethod.getDeclaringClass() : target.getClass());
            if (targetMethod.getAnnotation(SentinelResource.class) == null) {
                return pjp.proceed();
            }
            return super.invokeResourceWithSentinel(pjp);
        }
    }
}
