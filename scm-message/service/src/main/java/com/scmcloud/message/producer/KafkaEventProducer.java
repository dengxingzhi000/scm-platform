package com.scmcloud.message.producer;

import com.scmcloud.message.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String TOPIC_PREFIX = "scm.";
    
    public CompletableFuture<SendResult<String, String>> send(DomainEvent event) {
        String topic = TOPIC_PREFIX + event.getAggregateType().toLowerCase() + ".events";
        String key = event.getAggregateId();
        
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(topic, key, payload);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Event sent successfully: topic={}, key={}, eventType={}", 
                            topic, key, event.getEventType());
                } else {
                    log.error("Failed to send event: topic={}, key={}, eventType={}", 
                            topic, key, event.getEventType(), ex);
                }
            });
            
            return future;
        } catch (Exception e) {
            log.error("Failed to serialize event", e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
