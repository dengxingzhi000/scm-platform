package com.scmcloud.common.integration.events;

import com.scmcloud.common.integration.messaging.ReliableMessagePublisher;
import com.scmcloud.common.integration.model.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class UserLoginEventProducer {
    private final ReliableMessagePublisher publisher;

    public void publish(Long userId, String username, String ip, String deviceId, String location) {
        UserLoginEvent event = UserLoginEvent.builder()
                .userId(userId)
                .username(username)
                .ipAddress(ip)
                .deviceId(deviceId)
                .location(location)
                .loginTime(Instant.now())
                .build();
        MessageEnvelope<UserLoginEvent> envelope = MessageEnvelope.of(
                        "auth.user.login",
                        "auth-service",
                        event)
                .toBuilder()
                .subject(username)
                .build();
        publisher.sendSync(UserLoginEventChannels.EXCHANGE, UserLoginEventChannels.ROUTING_KEY, envelope);
    }
}
