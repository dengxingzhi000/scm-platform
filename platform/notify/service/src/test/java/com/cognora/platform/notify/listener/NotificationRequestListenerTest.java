package com.cognora.platform.notify.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cognora.platform.notify.api.request.NotificationRequest;
import com.cognora.platform.notify.service.ISysUserNotificationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationRequestListenerTest {

    private final ISysUserNotificationService service = mock(ISysUserNotificationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationRequestListener listener =
            new NotificationRequestListener(objectMapper, service);

    private String json(NotificationRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    @Test
    void createsTheNotificationWithTheProducerSuppliedIdentity() throws Exception {
        when(service.createIfAbsent(anyString(), anyString(), anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(true);

        listener.onNotificationRequest(json(new NotificationRequest()
                .setTenantId("t-1")
                .setUserId("u-1")
                .setNotifyType("INVENTORY")
                .setTitle("库存缺货提醒")
                .setContent("SKU-1 可用库存为 0")
                .setLink("/inventory/alerts")
                .setBusinessKey("inv-alert:a-1")));

        verify(service).createIfAbsent("t-1", "u-1", "库存缺货提醒", "SKU-1 可用库存为 0",
                "INVENTORY", "/inventory/alerts", "inv-alert:a-1");
    }

    @Test
    void normalisesTheTypeBecauseTheTableHasACheckConstraint() throws Exception {
        listener.onNotificationRequest(json(new NotificationRequest()
                .setTenantId("t-1").setUserId("u-1").setTitle("x").setNotifyType(" approval ")));

        verify(service).createIfAbsent(anyString(), anyString(), anyString(), any(), org.mockito.ArgumentMatchers.eq("APPROVAL"), any(), any());
    }

    @Test
    void fallsBackToSystemForAnUnknownType() throws Exception {
        listener.onNotificationRequest(json(new NotificationRequest()
                .setTenantId("t-1").setUserId("u-1").setTitle("x").setNotifyType("MARKETING")));

        verify(service).createIfAbsent(anyString(), anyString(), anyString(), any(),
                org.mockito.ArgumentMatchers.eq("SYSTEM"), any(), any());
    }

    @Test
    void ignoresARequestWithoutATenantSoItCannotWriteIntoAnotherTenant() throws Exception {
        assertThatCode(() -> listener.onNotificationRequest(json(new NotificationRequest()
                .setUserId("u-1").setTitle("x")))).doesNotThrowAnyException();

        verify(service, never()).createIfAbsent(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void ignoresARequestWithoutARecipient() throws Exception {
        assertThatCode(() -> listener.onNotificationRequest(json(new NotificationRequest()
                .setTenantId("t-1").setTitle("x")))).doesNotThrowAnyException();

        verify(service, never()).createIfAbsent(any(), any(), any(), any(), any(), any(), any());
    }
}
