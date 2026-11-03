package com.scmcloud.common.rest.aspect;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * @HttpExchange 闄嶇骇澶勭悊 AOP 鍒囬潰
 * <p>鏇夸唬 OpenFeign 锟紽allbackFactory</p>
 *
 * <p>鍔熻兘锟?
 * <ul>
 *   <li>鎷︽埅鎵€锟紷SentinelResource 娉ㄨВ鐨勬柟锟?li>
 *   <li>寮傚父鍒嗙被锛欱lockException锛堥檺娴侊級銆乀imeoutException锛堣秴鏃讹級銆佸叾浠栧紓锟?li>
 *   <li>鑷姩璋冪敤鎺ュ彛锟絛efault 闄嶇骇鏂规硶</li>
 *   <li>Micrometer 鎸囨爣璺熻釜</li>
 * </ul>
 *
 * <p>浣跨敤绀轰緥锟?
 * <pre>
 * {@code
 * @HttpExchange("/api/users")
 * public interface UserServiceClient {
 *     @GetExchange("/{id}")
 *     @SentinelResource(value = "user-service:getById", fallback = "getByIdFallback")
 *     ApiResponse<User> getById(@PathVariable Long id);
 *
 *     // 闄嶇骇鏂规硶锛坉efault锟?
 *     default ApiResponse<User> getByIdFallback(Long id, Throwable ex) {
 *         log.warn("User service fallback: id={}", id);
 *         return ApiResponse.fail(503, "Service unavailable");
 *     }
 * }
 * }
 * </pre>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class HttpExchangeFallbackAspect {
    private final MeterRegistry meterRegistry;

    /**
     * 鎷︽埅鎵€锟紷SentinelResource 娉ㄨВ鐨勬柟锟?
     */
    @Around("@annotation(sentinelResource)")
    public Object handleSentinelResource(
        ProceedingJoinPoint joinPoint,
        SentinelResource sentinelResource) throws Throwable {

        String resourceName = sentinelResource.value();
        String fallbackMethod = sentinelResource.fallback();

        try {
            // 鎵ц鍘熸柟锟?
            Object result = joinPoint.proceed();

            // 璁板綍鎴愬姛鎸囨爣
            meterRegistry.counter("http.exchange.success",
                                  "resource", resourceName).increment();

            return result;

        } catch (BlockException ex) {
            // Sentinel 闄愭祦/闄嶇骇
            meterRegistry.counter("http.exchange.blocked",
                                  "resource", resourceName).increment();
            log.warn("HTTP Exchange blocked by Sentinel: resource={}, reason={}",
                     resourceName, ex.getRule());

            return invokeFallback(joinPoint, fallbackMethod, ex);

        } catch (ResourceAccessException | SocketTimeoutException | TimeoutException ex) {
            // 瓒呮椂寮傚父
            meterRegistry.counter("http.exchange.timeout",
                                  "resource", resourceName).increment();
            log.error("HTTP Exchange timeout: resource={}, error={}",
                      resourceName, ex.getMessage());

            return invokeFallback(joinPoint, fallbackMethod, ex);

        } catch (Exception ex) {
            // 鍏朵粬寮傚父
            meterRegistry.counter("http.exchange.failure",
                                  "resource", resourceName,
                                  "exception", ex.getClass().getSimpleName()).increment();
            log.error("HTTP Exchange failed: resource={}, exception={}",
                      resourceName, ex.getClass().getSimpleName(), ex);

            return invokeFallback(joinPoint, fallbackMethod, ex);
        }
    }

    /**
     * 璋冪敤闄嶇骇鏂规硶
     *
     * <p>闄嶇骇鏂规硶绛惧悕锟?
     * <ul>
     *   <li>鍙傛暟锛氫笌鍘熸柟娉曠浉锟? Throwable ex</li>
     *   <li>杩斿洖鍊硷細涓庡師鏂规硶鐩稿悓</li>
     *   <li>淇グ绗︼細default锛堟帴鍙i粯璁ゆ柟娉曪級</li>
     * </ul>
     *
     * @param joinPoint 鍒囩偣
     * @param fallbackMethodName 闄嶇骇鏂规硶锟?
     * @param cause 寮傚父
     * @return 闄嶇骇缁撴灉
     */
    private Object invokeFallback(
        ProceedingJoinPoint joinPoint,
        String fallbackMethodName,
        Throwable cause) throws Throwable {

        if (fallbackMethodName == null || fallbackMethodName.isEmpty()) {
            // 鏃犻檷绾ф柟娉曪紝鐩存帴鎶涘嚭寮傚父
            throw cause;
        }

        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 鏌ユ壘闄嶇骇鏂规硶
        Method fallbackMethod = findFallbackMethod(
            signature.getDeclaringType(),
            fallbackMethodName,
            args
        );

        if (fallbackMethod != null) {
            try {
                // 璋冪敤闄嶇骇鏂规硶锛堟坊锟絋hrowable 鍙傛暟锟?
                Object[] fallbackArgs = buildFallbackArgs(args, cause);
                fallbackMethod.setAccessible(true);
                return fallbackMethod.invoke(target, fallbackArgs);
            } catch (Exception e) {
                log.error("Failed to invoke fallback method '{}': {}",
                          fallbackMethodName, e.getMessage(), e);
                throw cause;
            }
        } else {
            log.warn("Fallback method '{}' not found, rethrowing exception", fallbackMethodName);
            throw cause;
        }
    }

    /**
     * 鏌ユ壘闄嶇骇鏂规硶
     *
     * <p>鏌ユ壘绛栫暐锟?
     * <ol>
     *   <li>鏌ユ壘鎺ュ彛锟絛efault 鏂规硶</li>
     *   <li>鍙傛暟绛惧悕锛氬師鏂规硶鍙傛暟 + Throwable</li>
     * </ol>
     *
     * @param declaringType 澹版槑绫诲瀷
     * @param methodName 鏂规硶锟?
     * @param originalArgs 鍘熷鍙傛暟
     * @return 闄嶇骇鏂规硶锛屾湭鎵惧埌鍒欒繑锟絥ull
     */
    private Method findFallbackMethod(
        Class<?> declaringType,
        String methodName,
        Object[] originalArgs) {

        try {
            // 鏋勫缓鍙傛暟绫诲瀷鏁扮粍锛堝師鍙傛暟 + Throwable锟?
            Class<?>[] paramTypes = new Class[originalArgs.length + 1];
            for (int i = 0; i < originalArgs.length; i++) {
                paramTypes[i] = originalArgs[i] != null ?
                    originalArgs[i].getClass() : Object.class;
            }
            paramTypes[originalArgs.length] = Throwable.class;

            // 鍦ㄥ0鏄庢帴鍙d腑鏌ユ壘 default 鏂规硶
            return declaringType.getDeclaredMethod(methodName, paramTypes);

        } catch (NoSuchMethodException e) {
            // 灏濊瘯鏉炬暎鍖归厤锛堝弬鏁扮被鍨嬪彲鑳戒笉瀹屽叏鍖归厤锟?
            for (Method method : declaringType.getDeclaredMethods()) {
                if (method.getName().equals(methodName) &&
                    method.isDefault() &&
                    method.getParameterCount() == originalArgs.length + 1) {

                    // 妫€鏌ユ渶鍚庝竴涓弬鏁版槸鍚︿负 Throwable
                    Class<?>[] types = method.getParameterTypes();
                    if (Throwable.class.isAssignableFrom(types[types.length - 1])) {
                        return method;
                    }
                }
            }

            log.debug("Fallback method '{}' not found in {}", methodName, declaringType.getName());
            return null;
        }
    }

    /**
     * 鏋勫缓闄嶇骇鏂规硶鍙傛暟锛堟坊锟絋hrowable锟?
     *
     * @param originalArgs 鍘熷鍙傛暟
     * @param cause 寮傚父
     * @return 闄嶇骇鏂规硶鍙傛暟鏁扮粍
     */
    private Object[] buildFallbackArgs(Object[] originalArgs, Throwable cause) {
        Object[] fallbackArgs = new Object[originalArgs.length + 1];
        System.arraycopy(originalArgs, 0, fallbackArgs, 0, originalArgs.length);
        fallbackArgs[originalArgs.length] = cause;
        return fallbackArgs;
    }
}
