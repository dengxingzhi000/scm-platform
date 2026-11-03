package com.cognora.platform.inventory.service.notification;

import com.cognora.platform.inventory.domain.entity.InvAlert;
import com.cognora.platform.notify.api.request.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publishes stock alerts to scm-notify instead of calling it, so a slow notification
 * service can never block the scan job.
 */
@Slf4j
@Component
public class InventoryAlertNotificationPublisher {

    private static final int ALERT_TYPE_OUT_OF_STOCK = 2;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;
    private final List<String> configuredRecipients;

    public InventoryAlertNotificationPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${notify.request-topic:cognora.notify.events}") String topic,
            @Value("${cognora.inventory.alert.notify-user-ids:}") String configuredRecipients) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.configuredRecipients = split(configuredRecipients);
    }

    /**
     * @return true when every recipient got the event. False leaves {@code notified} unset, so the
     *         next scan retries instead of silently dropping the alert.
     */
    public boolean publish(InvAlert alert) {
        List<String> recipients = recipientsOf(alert);
        if (recipients.isEmpty()) {
            log.debug("No inventory-alert recipient configured, skipping alert {}", alert.getId());
            return false;
        }
        if (!StringUtils.hasText(alert.getTenantId())) {
            log.warn("Alert {} has no tenant, skipping notification", alert.getId());
            return false;
        }

        for (String userId : recipients) {
            // A fresh instance per recipient: the producer serialises asynchronously, so a shared
            // mutable request could be sent with another recipient's id.
            NotificationRequest request = new NotificationRequest()
                    .setTenantId(alert.getTenantId())
                    .setUserId(userId)
                    .setNotifyType("INVENTORY")
                    .setTitle(title(alert))
                    .setContent(content(alert))
                    .setLink("/inventory/alerts")
                    .setBusinessKey("inv-alert:" + alert.getId());
            try {
                kafkaTemplate.send(topic, alert.getId(), request).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while publishing alert {} to {}", alert.getId(), userId);
                return false;
            } catch (Exception e) {
                log.warn("Failed to publish alert {} to {}", alert.getId(), userId, e);
                return false;
            }
        }
        return true;
    }

    private List<String> recipientsOf(InvAlert alert) {
        List<String> explicit = split(alert.getNotifyToUserIds());
        return explicit.isEmpty() ? configuredRecipients : explicit;
    }

    private String title(InvAlert alert) {
        return alert.getAlertType() != null && alert.getAlertType() == ALERT_TYPE_OUT_OF_STOCK
                ? "库存缺货提醒"
                : "库存不足提醒";
    }

    private String content(InvAlert alert) {
        return "SKU " + alert.getSkuId() + " 在仓库 " + alert.getWarehouseId()
                + " 当前可用 " + alert.getCurrentStock()
                + "，安全库存 " + alert.getSafetyStock() + "，请及时补货";
    }

    private static List<String> split(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
