package com.scmcloud.order.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.order.domain.entity.OrdOrderEvent;
import com.scmcloud.order.mapper.OrdOrderEventMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
public class OrderEventStore {

    private final OrdOrderEventMapper eventMapper;
    private final ObjectMapper objectMapper;

    public OrderEventStore(OrdOrderEventMapper eventMapper, ObjectMapper objectMapper) {
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 追加事件。event_id 唯一约束保证幂等:重复事件记 warn 跳过。
     */
    public void append(OrderEvent event) {
        try {
            OrdOrderEvent entity = new OrdOrderEvent();
            entity.setTenantId(event.getTenantId());
            entity.setEventId(event.getEventId());
            entity.setOrderId(event.getOrderId());
            entity.setOrderNo(event.getOrderNo());
            entity.setEventType(event.getEventType());
            entity.setEventData(objectMapper.writeValueAsString(event));
            eventMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            log.warn("Duplicate order event ignored: eventId={}, orderId={}",
                    event.getEventId(), event.getOrderId());
        } catch (JsonProcessingException e) {
            throw new ServiceException("Failed to serialize order event: " + event.getEventId(), e);
        }
    }

    /**
     * 按写入顺序取全部事件(create_time ASC, id ASC 兜底保证稳定排序)。
     */
    public List<OrderEvent> getEvents(Long orderId) {
        LambdaQueryWrapper<OrdOrderEvent> wrapper = Wrappers.lambdaQuery(OrdOrderEvent.class)
                .eq(OrdOrderEvent::getOrderId, orderId)
                .orderByAsc(OrdOrderEvent::getCreateTime)
                .orderByAsc(OrdOrderEvent::getId);
        return eventMapper.selectList(wrapper).stream()
                .map(this::deserialize)
                .toList();
    }

    /**
     * 分页取事件(pageNo 从 1 开始),替代原 .last() 字符串拼接。
     */
    public List<OrderEvent> getEvents(Long orderId, int pageNo, int pageSize) {
        Page<OrdOrderEvent> result = eventMapper.selectPage(
                new Page<>(pageNo, pageSize),
                Wrappers.<OrdOrderEvent>lambdaQuery()
                        .eq(OrdOrderEvent::getOrderId, orderId)
                        .orderByAsc(OrdOrderEvent::getCreateTime)
                        .orderByAsc(OrdOrderEvent::getId));
        return result.getRecords().stream()
                .map(this::deserialize)
                .toList();
    }

    private OrderEvent deserialize(OrdOrderEvent entity) {
        try {
            return objectMapper.readValue(entity.getEventData(), OrderEvent.class);
        } catch (JsonProcessingException e) {
            throw new ServiceException(
                    "Failed to deserialize order event: " + entity.getEventId(), e);
        }
    }
}
