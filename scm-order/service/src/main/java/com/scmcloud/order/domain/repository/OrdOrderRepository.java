package com.scmcloud.order.domain.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.UUID;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.mapper.OrdOrderMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for the {@link OrdOrder} aggregate root.
 *
 * <p>仅做单表 CRUD 委托；订单事件由 {@code OrdOrderCommandService} 通过
 * {@code OrderEventStore} 写入 {@code ord_order_event} 表，无需在此层处理。</p>
 *
 * @author deng
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class OrdOrderRepository {
    private final OrdOrderMapper orderMapper;

    /**
     * 新增或更新订单。
     */
    public void save(OrdOrder order) {
        if (order.getId() == null) {
            orderMapper.insert(order);
        } else {
            orderMapper.updateById(order);
        }
    }

    /**
     * 按 ID 查找。
     */
    public OrdOrder findById(UUID id) {
        return orderMapper.selectById(id);
    }

    /**
     * 按订单号查找。
     */
    public OrdOrder findByOrderNo(String orderNo) {
        return orderMapper.selectOne(
                new LambdaQueryWrapper<OrdOrder>()
                        .eq(OrdOrder::getOrderNo, orderNo)
                        .eq(OrdOrder::getDeleted, false));
    }

    /**
     * 按用户 ID 查找（按创建时间倒序）。
     */
    public List<OrdOrder> findByUserId(String userId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<OrdOrder>()
                        .eq(OrdOrder::getUserId, userId)
                        .eq(OrdOrder::getDeleted, false)
                        .orderByDesc(OrdOrder::getCreateTime));
    }
}
