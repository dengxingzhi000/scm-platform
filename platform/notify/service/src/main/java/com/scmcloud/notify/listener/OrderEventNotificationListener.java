package com.scmcloud.notify.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.notify.service.ISysUserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Turns committed order events (published from the order outbox) into站内通知.
 *
 * <p>The outbox id is stable across redeliveries, so it doubles as the notification's
 * {@code businessKey}: a duplicate delivery is ignored instead of creating a second row.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventNotificationListener {

    private final ObjectMapper objectMapper;
    private final ISysUserNotificationService notificationService;

    @KafkaListener(
            topics = "${notify.order-events-topic:scm.ordorder}",
            groupId = "${spring.kafka.consumer.group-id:notify-order-events}")
    public void onOrderEvent(String message) throws Exception {
        JsonNode envelope = objectMapper.readTree(message);
        JsonNode outbox = readNested(envelope.get("data"));
        if (outbox == null || !"OrdOrder".equalsIgnoreCase(text(outbox, "aggregateType"))) {
            return;
        }

        JsonNode event = readNested(outbox.get("payload"));
        if (event == null) {
            return;
        }

        String tenantId = text(outbox, "tenantId");
        String userId = text(event, "userId");
        String eventType = text(outbox, "eventType");
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(userId)) {
            log.debug("Skip order event without recipient: type={}, outboxId={}",
                    eventType, text(outbox, "id"));
            return;
        }

        NotificationContent content = map(eventType, event);
        if (content == null) {
            return;
        }

        boolean created = notificationService.createIfAbsent(tenantId, userId,
                content.title(), content.body(), "ORDER", content.link(), text(outbox, "id"));
        log.info("Order event consumed: type={}, outboxId={}, userId={}, notificationCreated={}",
                eventType, text(outbox, "id"), userId, created);
    }

    private JsonNode readNested(JsonNode node) throws Exception {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isTextual() ? objectMapper.readTree(node.asText()) : node;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private NotificationContent map(String eventType, JsonNode event) {
        if (!"ORDER_STATUS_CHANGED".equals(eventType)) {
            return null;
        }
        String orderNo = text(event, "orderNo");
        String toStatus = text(event, "toStatus");
        if (!StringUtils.hasText(orderNo) || toStatus == null) {
            return null;
        }

        return switch (toStatus) {
            case "PAID" -> new NotificationContent("订单已支付", "订单 " + orderNo + " 支付成功");
            case "PENDING_SHIP" -> new NotificationContent("订单已确认", "订单 " + orderNo + " 已确认，等待仓库发货");
            case "SHIPPED" -> new NotificationContent("订单已发货", "订单 " + orderNo + " 已发货，请留意物流信息");
            case "IN_TRANSIT" -> new NotificationContent("订单运输中", "订单 " + orderNo + " 正在运输途中");
            case "DELIVERED" -> new NotificationContent("订单已送达", "订单 " + orderNo + " 已送达，请及时确认收货");
            case "COMPLETED" -> new NotificationContent("订单已完成", "订单 " + orderNo + " 已完成");
            case "CANCELLED" -> new NotificationContent("订单已取消", "订单 " + orderNo + " 已取消");
            case "REFUNDING" -> new NotificationContent("退款处理中", "订单 " + orderNo + " 的退款申请正在处理");
            case "REFUNDED" -> new NotificationContent("退款已完成", "订单 " + orderNo + " 的退款已完成");
            default -> null;
        };
    }

    private record NotificationContent(String title, String body, String link) {
        private NotificationContent(String title, String body) {
            this(title, body, "/order");
        }
    }
}
