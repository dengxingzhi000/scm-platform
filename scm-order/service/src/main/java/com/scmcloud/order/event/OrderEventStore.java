package com.scmcloud.order.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.order.domain.entity.OrdOrderEvent;
import com.scmcloud.order.mapper.OrdOrderEventMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class OrderEventStore {

    private final OrdOrderEventMapper eventMapper;
    private final ObjectMapper objectMapper;

    public OrderEventStore(OrdOrderEventMapper eventMapper, ObjectMapper objectMapper) {
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper;
    }

    public void append(OrderEvent event) {
        try {
            OrdOrderEvent entity = new OrdOrderEvent();
            entity.setEventId(event.getEventId());
            entity.setOrderId(event.getOrderId());
            entity.setEventType(event.getEventType());
            entity.setEventData(objectMapper.writeValueAsString(event));
            eventMapper.insert(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    public List<OrderEvent> getEvents(UUID orderId) {
        LambdaQueryWrapper<OrdOrderEvent> wrapper = Wrappers.lambdaQuery(OrdOrderEvent.class)
            .eq(OrdOrderEvent::getOrderId, orderId)
            .orderByAsc(OrdOrderEvent::getCreateTime);
        return eventMapper.selectList(wrapper).stream()
            .map(this::deserialize)
            .collect(Collectors.toList());
    }

    public List<OrderEvent> getEvents(UUID orderId, int offset, int limit) {
        LambdaQueryWrapper<OrdOrderEvent> wrapper = Wrappers.lambdaQuery(OrdOrderEvent.class)
            .eq(OrdOrderEvent::getOrderId, orderId)
            .orderByAsc(OrdOrderEvent::getCreateTime)
            .last("OFFSET " + offset + " LIMIT " + limit);
        return eventMapper.selectList(wrapper).stream()
            .map(this::deserialize)
            .collect(Collectors.toList());
    }

    private OrderEvent deserialize(OrdOrderEvent entity) {
        try {
            return (OrderEvent) objectMapper.readValue(entity.getEventData(), OrderEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize event: " + entity.getEventId(), e);
        }
    }
}
