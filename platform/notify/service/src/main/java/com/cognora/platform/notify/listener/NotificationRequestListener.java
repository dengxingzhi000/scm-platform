package com.cognora.platform.notify.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cognora.platform.notify.api.request.NotificationRequest;
import com.cognora.platform.notify.service.ISysUserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * Turns business notifications produced by other services into站内通知.
 *
 * <p>Producers publish to one shared topic instead of calling the service, so a slow or
 * unavailable notify instance can never hold up an approval or an inventory scan. The
 * {@code businessKey} makes a redelivery idempotent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequestListener {

    private static final Set<String> TYPES = Set.of("SYSTEM", "ORDER", "INVENTORY", "APPROVAL", "SECURITY");

    private final ObjectMapper objectMapper;
    private final ISysUserNotificationService notificationService;

    @KafkaListener(
            topics = "${notify.request-topic:cognora.notify.events}",
            groupId = "${notify.request-group-id:notify-requests}")
    public void onNotificationRequest(String message) throws Exception {
        NotificationRequest request = objectMapper.readValue(message, NotificationRequest.class);
        if (request == null
                || !StringUtils.hasText(request.getTenantId())
                || !StringUtils.hasText(request.getUserId())
                || !StringUtils.hasText(request.getTitle())) {
            log.warn("Skip notification request with missing tenant/user/title: {}", message);
            return;
        }

        boolean created = notificationService.createIfAbsent(
                request.getTenantId(),
                request.getUserId(),
                request.getTitle(),
                request.getContent(),
                normalizeType(request.getNotifyType()),
                request.getLink(),
                request.getBusinessKey());
        log.info("Notification request consumed: type={}, userId={}, businessKey={}, created={}",
                request.getNotifyType(), request.getUserId(), request.getBusinessKey(), created);
    }

    /** The table has a CHECK constraint, so an unexpected type must not reach the insert. */
    private String normalizeType(String notifyType) {
        String upper = notifyType == null ? "" : notifyType.trim().toUpperCase(Locale.ROOT);
        return TYPES.contains(upper) ? upper : "SYSTEM";
    }
}
