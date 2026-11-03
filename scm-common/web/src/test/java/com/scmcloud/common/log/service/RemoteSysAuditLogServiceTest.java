package com.scmcloud.common.log.service;

import com.scmcloud.common.log.dto.AuditLogRecord;
import com.scmcloud.common.rest.client.SysAuditServiceClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * The delivery failure is swallowed so it can never break a business operation. It must still be
 * counted, otherwise auditing can stop platform-wide without any signal.
 */
class RemoteSysAuditLogServiceTest {

    @Test
    void failedDeliveryShouldBeCounted() {
        SysAuditServiceClient client = mock(SysAuditServiceClient.class);
        doThrow(new IllegalStateException("403 Forbidden")).when(client).create(any());
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RemoteSysAuditLogService service = new RemoteSysAuditLogService(client, meterRegistry);

        service.record(AuditLogRecord.builder().operationType("LOGIN").build());

        assertEquals(1.0, meterRegistry.counter(RemoteSysAuditLogService.METRIC_DELIVERY_FAILED).count());
    }

    @Test
    void successfulDeliveryShouldNotBeCounted() {
        SysAuditServiceClient client = mock(SysAuditServiceClient.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RemoteSysAuditLogService service = new RemoteSysAuditLogService(client, meterRegistry);

        service.record(AuditLogRecord.builder().operationType("LOGIN").build());

        assertEquals(0.0, meterRegistry.counter(RemoteSysAuditLogService.METRIC_DELIVERY_FAILED).count());
    }
}
