package com.scmcloud.common.dubbo;

import com.scmcloud.common.tenant.TenantContextHolder;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantContextFilter Tests")
class TenantContextFilterTest {

    private static final String TENANT_ID_KEY = "tenant_id";

    private TenantContextFilter filter;

    @Mock
    private Invoker<?> invoker;

    @Mock
    private Invocation invocation;

    @BeforeEach
    void setUp() {
        filter = new TenantContextFilter();
        TenantContextHolder.clear();
        RpcContext.getServiceContext().clearAttachments();
        RpcContext.getClientAttachment().clearAttachments();
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        RpcContext.getServiceContext().clearAttachments();
        RpcContext.getClientAttachment().clearAttachments();
    }

    @Test
    @DisplayName("Provider: should set tenantId from RpcContext attachment")
    void shouldSetTenantIdFromRpcContextOnProviderSide() {
        // Given
        UUID expectedTenantId = UUID.randomUUID();
        RpcContext.getServiceContext().setAttachment(TENANT_ID_KEY, expectedTenantId.toString());
        URL url = URL.valueOf("dubbo://localhost:20880/test?side=provider");
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(invocation)).thenReturn(new AppResponse());

        // When
        filter.invoke(invoker, invocation);

        // Then
        verify(invoker).invoke(invocation);
    }

    @Test
    @DisplayName("Provider: should restore previous tenant context after invocation")
    void shouldRestorePreviousTenantContextAfterInvocation() {
        // Given
        UUID previousTenantId = UUID.randomUUID();
        UUID newTenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(previousTenantId);

        RpcContext.getServiceContext().setAttachment(TENANT_ID_KEY, newTenantId.toString());
        URL url = URL.valueOf("dubbo://localhost:20880/test?side=provider");
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(invocation)).thenAnswer(invocationOnCall -> {
            // During invocation, the new tenant should be set
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(newTenantId);
            return new AppResponse();
        });

        // When
        filter.invoke(invoker, invocation);

        // Then - previous tenant should be restored
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(previousTenantId);
    }

    @Test
    @DisplayName("Provider: should clear tenant context when no previous context exists")
    void shouldClearTenantContextWhenNoPreviousContextExists() {
        // Given
        UUID newTenantId = UUID.randomUUID();
        RpcContext.getServiceContext().setAttachment(TENANT_ID_KEY, newTenantId.toString());
        URL url = URL.valueOf("dubbo://localhost:20880/test?side=provider");
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(invocation)).thenReturn(new AppResponse());

        // When
        filter.invoke(invoker, invocation);

        // Then - tenant should be cleared (no previous context)
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Provider: should not throw on invalid tenantId in RpcContext")
    void shouldNotThrowOnInvalidTenantIdInRpcContext() {
        // Given
        RpcContext.getServiceContext().setAttachment(TENANT_ID_KEY, "invalid-uuid");
        URL url = URL.valueOf("dubbo://localhost:20880/test?side=provider");
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(invocation)).thenReturn(new AppResponse());

        // When & Then - should not throw, just log warning
        assertThatCode(() -> filter.invoke(invoker, invocation)).doesNotThrowAnyException();
        verify(invoker).invoke(invocation);
    }

    @Test
    @DisplayName("Provider: should handle missing tenantId gracefully")
    void shouldHandleMissingTenantIdGracefully() {
        // Given - no tenant_id attachment set
        URL url = URL.valueOf("dubbo://localhost:20880/test?side=provider");
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(invocation)).thenReturn(new AppResponse());

        // When & Then
        assertThatCode(() -> filter.invoke(invoker, invocation)).doesNotThrowAnyException();
        verify(invoker).invoke(invocation);
    }

    @Test
    @DisplayName("Provider: should handle blank tenantId gracefully")
    void shouldHandleBlankTenantIdGracefully() {
        // Given
        RpcContext.getServiceContext().setAttachment(TENANT_ID_KEY, "   ");
        URL url = URL.valueOf("dubbo://localhost:20880/test?side=provider");
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(invocation)).thenReturn(new AppResponse());

        // When & Then
        assertThatCode(() -> filter.invoke(invoker, invocation)).doesNotThrowAnyException();
        verify(invoker).invoke(invocation);
    }

    @Test
    @DisplayName("Consumer: should propagate tenantId to RpcContext attachment")
    void shouldPropagateTenantIdToRpcContextOnConsumerSide() {
        // Given
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        URL url = URL.valueOf("dubbo://localhost:20880/test?side=consumer");
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(invocation)).thenReturn(new AppResponse());

        // When
        filter.invoke(invoker, invocation);

        // Then
        assertThat(RpcContext.getClientAttachment().getAttachment(TENANT_ID_KEY))
                .isEqualTo(tenantId.toString());
    }

    @Test
    @DisplayName("Consumer: should not set attachment when tenantId is null")
    void shouldNotSetAttachmentWhenTenantIdIsNull() {
        // Given - no tenant set in TenantContextHolder
        URL url = URL.valueOf("dubbo://localhost:20880/test?side=consumer");
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(invocation)).thenReturn(new AppResponse());

        // When
        filter.invoke(invoker, invocation);

        // Then
        assertThat(RpcContext.getClientAttachment().getAttachment(TENANT_ID_KEY)).isNull();
    }
}
