package com.scmcloud.order.event;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.domain.entity.OrdOrderEvent;
import com.scmcloud.order.mapper.OrdOrderEventMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventStoreTest {

    @Mock
    private OrdOrderEventMapper eventMapper;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<OrdOrderEvent>> listWrapperCaptor;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<OrdOrderEvent>> pageWrapperCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private OrderEventStore store;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrdOrderEvent.class);
        store = new OrderEventStore(eventMapper, objectMapper);
    }

    private OrderCreatedEvent createdEvent() {
        return new OrderCreatedEvent(UUID.randomUUID(), java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "NO1001", "u-1",
                new BigDecimal("99.90"), new BigDecimal("89.90"));
    }

    private OrderStatusChangedEvent statusEvent() {
        return new OrderStatusChangedEvent(UUID.randomUUID(), java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "NO1001",
                OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);
    }

    private OrdOrderEvent entity(OrderEvent event) throws JsonProcessingException {
        OrdOrderEvent entity = new OrdOrderEvent();
        entity.setTenantId(event.getTenantId());
        entity.setEventId(event.getEventId());
        entity.setOrderId(event.getOrderId());
        entity.setOrderNo(event.getOrderNo());
        entity.setEventType(event.getEventType());
        entity.setEventData(objectMapper.writeValueAsString(event));
        return entity;
    }

    @Test
    void appendShouldPersistSerializedEventWithMetadata() {
        OrderEvent event = createdEvent();

        store.append(event);

        ArgumentCaptor<OrdOrderEvent> captor = ArgumentCaptor.forClass(OrdOrderEvent.class);
        verify(eventMapper).insert(captor.capture());
        OrdOrderEvent saved = captor.getValue();
        assertEquals(event.getEventId(), saved.getEventId());
        assertEquals(event.getTenantId(), saved.getTenantId());
        assertEquals(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), saved.getOrderId());
        assertEquals("NO1001", saved.getOrderNo());
        assertEquals("ORDER_CREATED", saved.getEventType());
        assertTrue(saved.getEventData().contains("\"eventType\":\"ORDER_CREATED\""));
    }

    @Test
    void appendShouldIgnoreDuplicateEventId() {
        when(eventMapper.insert(any(OrdOrderEvent.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertDoesNotThrow(() -> store.append(createdEvent()));
    }

    @Test
    void appendShouldWrapSerializationFailure() throws JsonProcessingException {
        ObjectMapper broken = org.mockito.Mockito.mock(ObjectMapper.class);
        when(broken.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});
        OrderEventStore brokenStore = new OrderEventStore(eventMapper, broken);

        assertThrows(ServiceException.class, () -> brokenStore.append(createdEvent()));
    }

    @Test
    void getEventsShouldQueryWithStableOrderAndMapRecords() throws JsonProcessingException {
        when(eventMapper.selectList(listWrapperCaptor.capture()))
                .thenReturn(List.of(entity(createdEvent()), entity(statusEvent())));

        List<OrderEvent> events = store.getEvents(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertEquals(2, events.size());
        assertInstanceOf(OrderCreatedEvent.class, events.get(0));
        assertInstanceOf(OrderStatusChangedEvent.class, events.get(1));

        String sql = listWrapperCaptor.getValue().getSqlSegment();
        assertTrue(sql.contains("order_id"));
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("create_time ASC"));
        assertTrue(sql.contains("id ASC"));
    }

    @Test
    void getEventsShouldWrapDeserializationFailure() {
        OrdOrderEvent bad = new OrdOrderEvent();
        bad.setEventId(UUID.randomUUID());
        bad.setEventData("not-json");
        when(eventMapper.selectList(any())).thenReturn(List.of(bad));

        assertThrows(ServiceException.class, () -> store.getEvents(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }

    @Test
    void pagedGetEventsShouldUsePageQuery() throws JsonProcessingException {
        Page<OrdOrderEvent> page = new Page<>(2, 10);
        page.setRecords(List.of(entity(createdEvent())));
        when(eventMapper.selectPage(any(), pageWrapperCaptor.capture())).thenReturn(page);

        List<OrderEvent> events = store.getEvents(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), 2, 10);

        assertEquals(1, events.size());

        String sql = pageWrapperCaptor.getValue().getSqlSegment();
        assertTrue(sql.contains("order_id"));
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("create_time ASC"));
        assertTrue(sql.contains("id ASC"));

        verify(eventMapper).selectPage(
                argThat(p -> p instanceof Page<?> pg && pg.getCurrent() == 2 && pg.getSize() == 10),
                any());
    }
}
