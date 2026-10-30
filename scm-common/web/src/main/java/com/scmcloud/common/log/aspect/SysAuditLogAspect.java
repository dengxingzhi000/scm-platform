package com.scmcloud.common.log.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.log.annotation.AuditLog;
import com.scmcloud.common.log.dto.AuditLogRecord;
import com.scmcloud.common.log.service.ISysAuditLogService;
import com.scmcloud.common.security.util.DesensitizeUtils;
import com.scmcloud.common.security.util.IpUtils;
import com.scmcloud.common.web.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Records {@link AuditLog}-annotated operations through {@link ISysAuditLogService}.
 *
 * <p>Lives in the shared library and is registered by AuditLogAutoConfiguration: the previous
 * aspect was component-scanned only inside scm-audit, so the annotation on system/auth controllers
 * never produced a record.
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class SysAuditLogAspect {

    private final ISysAuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint point, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) point.getSignature();
        AuditLog annotation = signature.getMethod().getAnnotation(AuditLog.class);
        if (annotation == null) {
            annotation = auditLog;
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        AuditLogRecord record = AuditLogRecord.builder()
                .userId(SecurityUtils.getCurrentUserUuid().map(Object::toString).orElse(null))
                .username(SecurityUtils.getCurrentUsername().orElse(null))
                .operationType(annotation.businessType())
                .operationDesc(annotation.operation())
                .riskLevel(annotation.riskLevel())
                .build();

        if (request != null) {
            record.setRequestUri(request.getRequestURI());
            record.setRequestMethod(request.getMethod());
            record.setIpAddress(IpUtils.getClientIp(request));
            record.setUserAgent(request.getHeader("User-Agent"));

            if (annotation.recordParams()) {
                try {
                    String params = objectMapper.writeValueAsString(point.getArgs());
                    record.setRequestParams(DesensitizeUtils.desensitize(params));
                } catch (Exception e) {
                    log.error("Failed to serialize request params", e);
                }
            }
        }

        Object result;
        try {
            result = point.proceed();
            record.setStatus(1);

            if (annotation.recordResult() && result != null) {
                try {
                    record.setResponseData(DesensitizeUtils.desensitize(objectMapper.writeValueAsString(result)));
                } catch (Exception e) {
                    log.error("Failed to serialize response", e);
                }
            }
        } catch (Throwable e) {
            record.setStatus(0);
            record.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            record.setExecuteTime((int) (System.currentTimeMillis() - startTime));
            record.setCreateTime(LocalDateTime.now());
            try {
                auditLogService.record(record);
            } catch (Exception e) {
                log.error("Failed to record audit log", e);
            }
        }

        return result;
    }
}
