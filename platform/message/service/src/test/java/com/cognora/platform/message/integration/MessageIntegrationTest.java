package com.cognora.platform.message.integration;

import com.cognora.platform.message.ScmMessageApplication;
import com.cognora.platform.message.event.DomainEvent;
import com.cognora.platform.message.event.OrderCreatedEvent;
import com.cognora.platform.message.producer.OutboxService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ScmMessageApplication.class)
@Disabled("Pre-existing: requires running database")
class MessageIntegrationTest {
    
    @Autowired
    private OutboxService outboxService;
    
    @Test
    void shouldSaveEventToOutbox() {
        // Given
        DomainEvent event = OrderCreatedEvent.of("order-1", "ORD001", "019a0aee-0000-7000-8000-000000000002");
        
        // When
        var result = outboxService.saveEvent(event);
        
        // Then
        assertNotNull(result);
        assertEquals("ORDER_CREATED", result.getEventType());
        assertEquals("PENDING", result.getStatus());
    }
}
