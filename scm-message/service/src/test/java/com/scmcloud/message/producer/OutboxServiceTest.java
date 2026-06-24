package com.scmcloud.message.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.message.entity.EventOutbox;
import com.scmcloud.message.event.DomainEvent;
import com.scmcloud.message.event.OrderCreatedEvent;
import com.scmcloud.message.mapper.EventOutboxMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {
    
    @Mock
    private EventOutboxMapper mapper;
    
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @InjectMocks
    private OutboxService outboxService;
    
    @Test
    void shouldSaveEventToOutbox() {
        // Given
        DomainEvent event = OrderCreatedEvent.of("order-1", "ORD001", 1L);
        when(mapper.insert(any(EventOutbox.class))).thenReturn(1);
        
        // When
        EventOutbox result = outboxService.saveEvent(event);
        
        // Then
        assertNotNull(result);
        assertEquals("ORDER_CREATED", result.getEventType());
        assertEquals("PENDING", result.getStatus());
        verify(mapper).insert(any(EventOutbox.class));
    }
    
    @Test
    void shouldMarkAsPublished() {
        // Given
        String eventId = "event-1";
        EventOutbox outbox = new EventOutbox();
        outbox.setId(eventId);
        outbox.setStatus("PENDING");
        when(mapper.selectById(eventId)).thenReturn(outbox);
        when(mapper.updateById(any())).thenReturn(1);
        
        // When
        outboxService.markAsPublished(eventId);
        
        // Then
        assertEquals("PUBLISHED", outbox.getStatus());
        assertNotNull(outbox.getPublishedAt());
        verify(mapper).updateById(outbox);
    }
}
