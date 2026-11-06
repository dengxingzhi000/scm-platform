package com.cognora.platform.common.log.service;

import com.cognora.platform.common.log.dto.AuditLogRecord;
import com.cognora.platform.common.rest.client.SysAuditServiceClient;
import com.cognora.platform.common.util.UUIDv7Util;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Delivers audit records to scm-audit over HTTP instead of writing them locally.
 *
 * <p>scm-audit owns {@code db_audit}; every other service now reports through this sink, so
 * {@code @AuditLog} works wherever the shared library is on the classpath (previously the only
 * aspect lived inside scm-audit and was never scanned by the business services).
 *
 * <p>Delivery is best-effort: a failed audit call is logged and swallowed so it can never break
 * the business operation. Calls made before a token exists (e.g. login failures) have no
 * authenticated context and will fall back to the log line.
 *
 * <p>Because the failure is swallowed, every rejected delivery is counted
 * ({@code security.audit.delivery.failed}) so an operator can see that auditing stopped on this
 * service instead of only finding a WARN line in the log.
 *
 * <p>A connection-level failure is retried exactly once after a short pause; the same nonce is
 * reused, so a request that reached scm-audit but lost its response cannot create a second row.
 * Timeouts are not retried.
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteSysAuditLogService implements ISysAuditLogService {

    /** Deliveries that scm-audit refused or that never arrived; watch it per service. */
    static final String METRIC_DELIVERY_FAILED = "security.audit.delivery.failed";

    /** Pause before the single connection retry. */
    static final long RETRY_DELAY_MILLIS = 150L;

    private final SysAuditServiceClient auditServiceClient;

    private final @Nullable MeterRegistry meterRegistry;

    @Override
    public void record(AuditLogRecord record) {
        if (record.getCreateTime() == null) {
            record.setCreateTime(LocalDateTime.now());
        }
        // 一条记录只生成一个 nonce，重试复用同一个值：请求已经在 scm-audit 落库但响应丢失时，
        // 第二次投递会被唯一索引识别为重复，而不是多出一行。
        String nonce = UUIDv7Util.generate().toString().replace("-", "");
        try {
            auditServiceClient.create(record, nonce);
        } catch (Exception first) {
            if (!isConnectionFailure(first) || !sleepBeforeRetry()) {
                countFailedDelivery(record, first);
                return;
            }
            log.warn("Audit record delivery hit a connection failure, retrying once: type={}, desc={}",
                    sanitize(record.getOperationType()), sanitize(record.getOperationDesc()));
            try {
                auditServiceClient.create(record, nonce);
            } catch (Exception retried) {
                countFailedDelivery(record, retried);
            }
        }
    }

    /**
     * 只有连接类失败才重试一次：请求根本没到达 scm-audit，重试不可能产生重复行。
     *
     * <p>超时类失败故意不重试——请求可能已经被处理，放宽窗口只会给正在恢复的服务加压；
     * 即使以后放宽，nonce 的幂等也已经兜住重复写入。
     */
    static boolean isConnectionFailure(Throwable error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof ConnectException
                    || cause instanceof NoRouteToHostException
                    || cause instanceof UnknownHostException) {
                return true;
            }
            Throwable next = cause.getCause();
            if (next == cause) {
                return false;
            }
            cause = next;
        }
        return false;
    }

    /** 重试前短暂停顿；线程被中断（例如服务正在关闭）就放弃重试。 */
    private static boolean sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void countFailedDelivery(AuditLogRecord record, Exception e) {
        if (meterRegistry != null) {
            meterRegistry.counter(METRIC_DELIVERY_FAILED).increment();
        }
        log.warn("Audit record delivery failed: type={}, desc={}, user={}: {}",
                sanitize(record.getOperationType()), sanitize(record.getOperationDesc()),
                sanitize(record.getUsername()), e.getMessage());
    }

    @Override
    public void recordLogin(UUID userId, String username, String ipAddress, boolean success, String remark) {
        record(AuditLogRecord.builder()
                .userId(userId != null ? userId.toString() : null)
                .username(username)
                .ipAddress(ipAddress)
                .operationType(success ? "LOGIN" : "LOGIN_FAILURE")
                .operationDesc(success ? "用户登录" : "用户登录失败")
                .status(success ? 1 : 0)
                .errorMsg(success ? null : remark)
                .riskLevel(1)
                .build());
    }

    @Override
    public void recordLoginFailure(String username, String ipAddress, String reason) {
        record(AuditLogRecord.builder()
                .username(username)
                .ipAddress(ipAddress)
                .operationType("LOGIN_FAILURE")
                .operationDesc("用户登录失败")
                .errorMsg(reason)
                .status(0)
                .riskLevel(2)
                .build());
    }

    @Override
    public void recordLogout(UUID userId, String remark) {
        record(AuditLogRecord.builder()
                .userId(userId != null ? userId.toString() : null)
                .operationType("LOGOUT")
                .operationDesc("用户登出")
                .status(1)
                .riskLevel(1)
                .build());
    }

    @Override
    public void recordSecurityEvent(String eventType, Integer riskLevel, UUID userId, String username,
                                    String ipAddress, String resource, boolean success, String details) {
        record(AuditLogRecord.builder()
                .userId(userId != null ? userId.toString() : null)
                .username(username)
                .ipAddress(ipAddress)
                .operationType(eventType)
                .operationModule("SECURITY")
                .operationDesc(details)
                .requestUri(resource)
                .riskLevel(riskLevel)
                .status(success ? 1 : 0)
                .errorMsg(success ? null : details)
                .build());
    }

    private static String sanitize(String value) {
        return (value + "").replace("\r", "_").replace("\n", "_");
    }
}
