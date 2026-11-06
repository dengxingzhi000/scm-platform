package com.cognora.platform.common.log.service;

import com.cognora.platform.common.log.dto.AuditLogRecord;
import com.cognora.platform.common.rest.client.SysAuditServiceClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The delivery failure is swallowed so it can never break a business operation. It must still be
 * counted, otherwise auditing can stop platform-wide without any signal.
 */
class RemoteSysAuditLogServiceTest {

    /** What RestClient actually throws when the connection cannot be established. */
    private static ResourceAccessException connectionFailure() {
        return new ResourceAccessException("I/O error on POST request",
                new ConnectException("Connection refused"));
    }

    @Test
    void failedDeliveryShouldBeCounted() {
        SysAuditServiceClient client = mock(SysAuditServiceClient.class);
        doThrow(new IllegalStateException("403 Forbidden")).when(client).create(any(), anyString());
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RemoteSysAuditLogService service = new RemoteSysAuditLogService(client, meterRegistry);

        service.record(AuditLogRecord.builder().operationType("LOGIN").build());

        // 403 is not a connection failure: no retry, one counted failure.
        verify(client, times(1)).create(any(), anyString());
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

    @Test
    void connectionFailureIsRetriedOnceWithTheSameNonce() {
        SysAuditServiceClient client = mock(SysAuditServiceClient.class);
        doThrow(connectionFailure()).doNothing().when(client).create(any(), anyString());
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RemoteSysAuditLogService service = new RemoteSysAuditLogService(client, meterRegistry);

        service.record(AuditLogRecord.builder().operationType("LOGIN").build());

        ArgumentCaptor<String> nonces = ArgumentCaptor.forClass(String.class);
        verify(client, times(2)).create(any(), nonces.capture());
        List<String> sent = nonces.getAllValues();
        // A fresh nonce per attempt would be a fresh idempotency key on the receiving side,
        // so the retry would be able to write a second row.
        assertEquals(sent.get(0), sent.get(1));
        assertEquals(32, sent.get(0).length());
        assertEquals(0.0, meterRegistry.counter(RemoteSysAuditLogService.METRIC_DELIVERY_FAILED).count());
    }

    @Test
    void aRetryThatAlsoFailsIsCountedOnce() {
        SysAuditServiceClient client = mock(SysAuditServiceClient.class);
        doThrow(connectionFailure()).doThrow(connectionFailure()).when(client).create(any(), anyString());
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RemoteSysAuditLogService service = new RemoteSysAuditLogService(client, meterRegistry);

        service.record(AuditLogRecord.builder().operationType("LOGIN").build());

        verify(client, times(2)).create(any(), anyString());
        assertEquals(1.0, meterRegistry.counter(RemoteSysAuditLogService.METRIC_DELIVERY_FAILED).count());
    }

    @Test
    void timeoutsAreNotRetried() {
        SysAuditServiceClient client = mock(SysAuditServiceClient.class);
        doThrow(new ResourceAccessException("Read timed out",
                new java.net.SocketTimeoutException("Read timed out")))
                .when(client).create(any(), anyString());
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RemoteSysAuditLogService service = new RemoteSysAuditLogService(client, meterRegistry);

        service.record(AuditLogRecord.builder().operationType("LOGIN").build());

        verify(client, times(1)).create(any(), anyString());
        assertEquals(1.0, meterRegistry.counter(RemoteSysAuditLogService.METRIC_DELIVERY_FAILED).count());
    }
}
