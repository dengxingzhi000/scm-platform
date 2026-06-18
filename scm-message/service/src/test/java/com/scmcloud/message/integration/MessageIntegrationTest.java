package com.scmcloud.message.integration;

import com.scmcloud.message.ScmMessageApplication;
import com.scmcloud.message.event.DomainEvent;
import com.scmcloud.message.event.OrderCreatedEvent;
import com.scmcloud.message.producer.OutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ScmMessageApplication.class)
class MessageIntegrationTest {
    
    @Autowired
    private OutboxService outboxService;
    
    @Test
    void shouldSaveEventToOutbox() {
        // Given
        DomainEvent event = OrderCreatedEvent.of("order-1", "ORD001", 1L);
        
        // When
        var result = outboxService.saveEvent(event);
        
        // Then
        assertNotNull(result);
        assertEquals("ORDER_CREATED", result.getEventType());
        assertEquals("PENDING", result.getStatus());
    }
}
