package com.scmcloud.audit.controller;

import com.scmcloud.audit.domain.entity.SysAuditLog;
import com.scmcloud.audit.domain.entity.SysSensitiveOperationLog;
import com.scmcloud.audit.service.ISysAuditLogService;
import com.scmcloud.audit.service.ISysSensitiveOperationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审计 Controller 的对外契约：只读查询保留，HTTP 创建/删除入口不再暴露。
 */
@ExtendWith(MockitoExtension.class)
class AuditControllerContractTest {

    @Mock
    private ISysAuditLogService auditLogService;

    @Mock
    private ISysSensitiveOperationLogService sensitiveOperationLogService;

    @InjectMocks
    private SysAuditLogController auditLogController;

    @InjectMocks
    private SysSensitiveOperationLogController sensitiveController;

    @Test
    void auditLogControllerShouldNotExposeDeleteEndpoint() {
        for (Method method : SysAuditLogController.class.getDeclaredMethods()) {
            assertNull(AnnotatedElementUtils.findMergedAnnotation(method, DeleteMapping.class),
                    () -> "unexpected DELETE mapping: " + method.getName());
        }
    }

    @Test
    void sensitiveLogControllerShouldNotExposeCreateOrDeleteEndpoint() {
        for (Method method : SysSensitiveOperationLogController.class.getDeclaredMethods()) {
            assertNull(AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class),
                    () -> "unexpected POST mapping: " + method.getName());
            assertNull(AnnotatedElementUtils.findMergedAnnotation(method, DeleteMapping.class),
                    () -> "unexpected DELETE mapping: " + method.getName());
        }
    }

    @Test
    void auditLogWriteChannelShouldStillDelegateToTheService() {
        SysAuditLog entity = SysAuditLog.builder().userId("user-1").operationType("LOGIN").build();
        when(auditLogService.createLog(entity)).thenReturn(entity);

        assertSame(entity, auditLogController.create(entity));
        verify(auditLogService).createLog(entity);
    }

    @Test
    void readEndpointsShouldRemainAvailable() {
        SysAuditLog log = SysAuditLog.builder().id("log-1").build();
        when(auditLogService.getById("log-1")).thenReturn(log);
        when(auditLogService.listByUserId("user-1")).thenReturn(List.of(log));
        when(auditLogService.listByModule("ORDER")).thenReturn(List.of(log));
        when(auditLogService.listByBusinessType("ORDER")).thenReturn(List.of(log));

        assertSame(log, auditLogController.getById("log-1"));
        assertTrue(auditLogController.listByUserId("user-1").contains(log));
        assertTrue(auditLogController.listByModule("ORDER").contains(log));
        assertTrue(auditLogController.listByBusinessType("ORDER").contains(log));
    }

    @Test
    void sensitiveReadEndpointsShouldRemainAvailable() {
        SysSensitiveOperationLog log = new SysSensitiveOperationLog().setId("log-1");
        when(sensitiveOperationLogService.getById("log-1")).thenReturn(log);
        when(sensitiveOperationLogService.listByRiskLevel(3)).thenReturn(List.of(log));

        assertSame(log, sensitiveController.getById("log-1"));
        assertTrue(sensitiveController.listByRiskLevel(3).contains(log));
    }
}
