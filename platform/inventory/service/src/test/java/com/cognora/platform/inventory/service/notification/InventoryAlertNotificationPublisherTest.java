package com.cognora.platform.inventory.service.notification;

import com.cognora.platform.inventory.domain.entity.InvAlert;
import com.cognora.platform.notify.api.request.NotificationRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryAlertNotificationPublisherTest {

    private static final String TOPIC = "cognora.notify.events";

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

    private InvAlert alert() {
        return new InvAlert()
                .setId("a-1")
                .setTenantId("t-1")
                .setSkuId("sku-1")
                .setWarehouseId("wh-1")
                .setAlertType(2)
                .setCurrentStock(0)
                .setSafetyStock(20);
    }

    private void brokerAccepts() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(null));
    }

    @Test
    void sendsToTheUsersNamedOnTheAlert() {
        brokerAccepts();
        InventoryAlertNotificationPublisher publisher =
                new InventoryAlertNotificationPublisher(kafkaTemplate, TOPIC, "");

        boolean sent = publisher.publish(alert().setNotifyToUserIds("u-1, u-2"));

        assertThat(sent).isTrue();
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, times(2)).send(eq(TOPIC), eq("a-1"), payload.capture());

        NotificationRequest request = (NotificationRequest) payload.getAllValues().get(0);
        assertThat(request.getTenantId()).isEqualTo("t-1");
        assertThat(request.getNotifyType()).isEqualTo("INVENTORY");
        assertThat(request.getTitle()).isEqualTo("库存缺货提醒");
        assertThat(request.getContent()).contains("sku-1").contains("当前可用 0").contains("安全库存 20");
        assertThat(request.getLink()).isEqualTo("/inventory/alerts");
        assertThat(request.getBusinessKey()).isEqualTo("inv-alert:a-1");
        assertThat(payload.getAllValues())
                .extracting(value -> ((NotificationRequest) value).getUserId())
                .containsExactly("u-1", "u-2");
    }

    @Test
    void fallsBackToTheConfiguredRecipients() {
        brokerAccepts();
        InventoryAlertNotificationPublisher publisher =
                new InventoryAlertNotificationPublisher(kafkaTemplate, TOPIC, "ops-1");

        assertThat(publisher.publish(alert())).isTrue();

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("a-1"), payload.capture());
        assertThat(((NotificationRequest) payload.getValue()).getUserId()).isEqualTo("ops-1");
    }

    @Test
    void staysSilentWhenNobodyIsConfigured() {
        InventoryAlertNotificationPublisher publisher =
                new InventoryAlertNotificationPublisher(kafkaTemplate, TOPIC, "  ");

        assertThat(publisher.publish(alert())).isFalse();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void reportsFailureSoTheNextScanRetries() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenAnswer(invocation -> CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        InventoryAlertNotificationPublisher publisher =
                new InventoryAlertNotificationPublisher(kafkaTemplate, TOPIC, "");

        assertThat(publisher.publish(alert().setNotifyToUserIds("u-1"))).isFalse();
    }

    @Test
    void titlesALowStockAlertDifferently() {
        brokerAccepts();
        InventoryAlertNotificationPublisher publisher =
                new InventoryAlertNotificationPublisher(kafkaTemplate, TOPIC, "");

        publisher.publish(alert().setAlertType(1).setNotifyToUserIds("u-1"));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("a-1"), payload.capture());
        assertThat(((NotificationRequest) payload.getValue()).getTitle()).isEqualTo("库存不足提醒");
    }
}
