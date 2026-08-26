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
     * <p>注意:PostgreSQL 中唯一约束冲突会使当前事务中止,吞掉 DuplicateKeyException 仅在同事务首次冲突可容忍;若未来需要重放历史事件再入库,请改用 REQUIRES_NEW 事务。</p>
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
        }
        // 幂等依赖 uk_ord_order_event_event_id;若未来新增其他唯一约束需重新评估此处吞异常的范围
        catch (DuplicateKeyException e) {
            log.warn("Duplicate order event ignored: eventId={}, orderId={}",
                    event.getEventId(), event.getOrderId());
        } catch (JsonProcessingException e) {
            throw new ServiceException("Failed to serialize order event: " + event.getEventId(), e);
        }
    }

    /**
     * 按写入顺序取全部事件(create_time ASC, id ASC 兜底保证稳定排序)。
     * 全量加载无上限,大订单量场景请用分页重载。
     */
    public List<OrderEvent> getEvents(Long orderId) {
        return eventMapper.selectList(byOrderId(orderId)).stream()
                .map(this::deserialize)
                .toList();
    }

    /**
     * 分页取事件(pageNo 从 1 开始),替代原 .last() 字符串拼接。
     * pageSize 上限受全局 PaginationInnerInterceptor maxLimit(1000) 约束。
     */
    public List<OrderEvent> getEvents(Long orderId, int pageNo, int pageSize) {
        Page<OrdOrderEvent> result = eventMapper.selectPage(
                new Page<>(pageNo, pageSize), byOrderId(orderId));
        return result.getRecords().stream()
                .map(this::deserialize)
                .toList();
    }

    private LambdaQueryWrapper<OrdOrderEvent> byOrderId(Long orderId) {
        return Wrappers.lambdaQuery(OrdOrderEvent.class)
                .eq(OrdOrderEvent::getOrderId, orderId)
                .orderByAsc(OrdOrderEvent::getCreateTime)
                .orderByAsc(OrdOrderEvent::getId);
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
