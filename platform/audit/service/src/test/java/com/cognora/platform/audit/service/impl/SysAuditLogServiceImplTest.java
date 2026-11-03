package com.cognora.platform.audit.service.impl;

import com.cognora.platform.audit.domain.entity.SysAuditLog;
import com.cognora.platform.audit.domain.entity.SysSensitiveOperationLog;
import com.cognora.platform.audit.mapper.SysAuditLogMapper;
import com.cognora.platform.audit.service.command.SysSensitiveOperationLogCommandService;
import com.cognora.platform.common.log.dto.AuditLogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysAuditLogServiceImplTest {

    private SysAuditLogMapper auditLogMapper;
    private SysSensitiveOperationLogCommandService sensitiveCommandService;
    private SysAuditLogServiceImpl service;

    @BeforeEach
    void setUp() {
        auditLogMapper = mock(SysAuditLogMapper.class);
        sensitiveCommandService = mock(SysSensitiveOperationLogCommandService.class);
        service = new SysAuditLogServiceImpl(sensitiveCommandService);
        ReflectionTestUtils.setField(service, "baseMapper", auditLogMapper);
        when(auditLogMapper.insert(any(SysAuditLog.class))).thenReturn(1);
        when(sensitiveCommandService.createLog(any(SysSensitiveOperationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AuditLogRecord record(int riskLevel, String userId) {
        return AuditLogRecord.builder()
                .userId(userId)
                .username("admin")
                .operationType("ROLE_GRANT")
                .operationDesc("角色授权")
                .businessType("ROLE")
                .riskLevel(riskLevel)
                .status(1)
                .ipAddress("192.168.0.110")
                .createTime(LocalDateTime.now())
                .build();
    }

    @Test
    void highRiskOperation_isMirroredIntoTheSensitiveLedger() {
        service.record(record(4, "019a0aee-0000-7000-8000-000000000001"));

        ArgumentCaptor<SysSensitiveOperationLog> captor =
                ArgumentCaptor.forClass(SysSensitiveOperationLog.class);
        verify(sensitiveCommandService).createLog(captor.capture());
        SysSensitiveOperationLog sensitive = captor.getValue();
        assertThat(sensitive.getUserId()).isEqualTo("019a0aee-0000-7000-8000-000000000001");
        assertThat(sensitive.getOperationType()).isEqualTo("ROLE_GRANT");
        assertThat(sensitive.getSensitiveDataType()).isEqualTo("ROLE");
        assertThat(sensitive.getTargetTable()).isEqualTo("ROLE");
        assertThat(sensitive.getRiskScore()).isEqualTo(4);
        assertThat(sensitive.getApprovalRequired()).isFalse();
        assertThat(sensitive.getIpAddress()).isEqualTo("192.168.0.110");
    }

    @Test
    void lowRiskOperation_staysInTheGeneralAuditLogOnly() {
        service.record(record(1, "019a0aee-0000-7000-8000-000000000001"));

        verify(auditLogMapper).insert(any(SysAuditLog.class));
        verify(sensitiveCommandService, never()).createLog(any());
    }

    @Test
    void highRiskOperationWithoutAnActor_isNotMirrored() {
        // user_id is NOT NULL in the ledger; the general audit row still records the event.
        service.record(record(4, null));

        verify(auditLogMapper).insert(any(SysAuditLog.class));
        verify(sensitiveCommandService, never()).createLog(any());
    }
}
