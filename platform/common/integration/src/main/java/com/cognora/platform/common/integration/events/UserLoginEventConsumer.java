package com.cognora.platform.common.integration.events;

import com.cognora.platform.common.integration.messaging.InstrumentedMessageConsumer;
import com.cognora.platform.common.integration.model.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserLoginEventConsumer {
    private final InstrumentedMessageConsumer consumer;

    @RabbitListener(queues = UserLoginEventChannels.QUEUE)
    public void onMessage(@Payload MessageEnvelope<UserLoginEvent> envelope) {
        consumer.consume("user-login-consumer", envelope, event ->
                log.info("Received user login event userId={}, ip={}", event.getUserId(), event.getIpAddress())
        );
    }
}
