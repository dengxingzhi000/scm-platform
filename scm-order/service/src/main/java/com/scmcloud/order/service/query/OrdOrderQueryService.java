package com.scmcloud.order.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrdOrderItem;
import com.scmcloud.order.dto.OrderQueryRequest;
import com.scmcloud.order.mapper.OrdOrderItemMapper;
import com.scmcloud.order.mapper.OrdOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdOrderQueryService {
    private final OrdOrderMapper ordOrderMapper;
    private final OrdOrderItemMapper ordOrderItemMapper;

    @Slave
    public OrdOrder getById(String id) {
        return ordOrderMapper.selectById(id);
    }

    @Slave
    public List<OrdOrder> list() {
        return ordOrderMapper.selectList(null);
    }

    @Slave
    public Page<OrdOrder> page(Page<OrdOrder> page) {
        return ordOrderMapper.selectPage(page, null);
    }

    @Slave
    public List<OrdOrder> listByUserId(String userId) {
        LambdaQueryWrapper<OrdOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrder::getUserId, userId)
                .eq(OrdOrder::getDeleted, false)
                .orderByDesc(OrdOrder::getCreateTime);
        return ordOrderMapper.selectList(wrapper);
    }

    @Slave
    public Page<OrdOrder> pageByUserId(String userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<OrdOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrder::getUserId, userId)
                .eq(OrdOrder::getDeleted, false)
                .orderByDesc(OrdOrder::getCreateTime);
        return ordOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 自定义条件分页查询。
     *
     * @deprecated 调用方应改用 {@link #query(OrderQueryRequest)}，由本服务统一构造查询条件
     *             （含 {@code deleted = false} 过滤与排序），避免在 controller 中拼装 wrapper。
     */
    @Deprecated
    @Slave
    public Page<OrdOrder> pageWithWrapper(Page<OrdOrder> page, LambdaQueryWrapper<OrdOrder> wrapper) {
        return ordOrderMapper.selectPage(page, wrapper);
    }

    /**
     * 统一条件分页查询（查询条件构造下沉到服务层）。
     *
     * <p>根据 {@link OrderQueryRequest} 拼装过滤条件（订单号/用户/状态/来源/时间区间），
     * 强制包含 {@code deleted = false} 过滤，并按创建时间倒序。与退款、支付查询服务保持一致。</p>
     */
    @Slave
    public Page<OrdOrder> query(OrderQueryRequest request) {
        LambdaQueryWrapper<OrdOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(request.getOrderNo() != null, OrdOrder::getOrderNo, request.getOrderNo())
                .eq(request.getUserId() != null, OrdOrder::getUserId, request.getUserId())
                .eq(request.getStatus() != null, OrdOrder::getStatus, request.getStatus().getCode())
                .eq(request.getOrderSource() != null, OrdOrder::getOrderSource, request.getOrderSource())
                .ge(request.getStartTime() != null, OrdOrder::getCreateTime, request.getStartTime())
                .le(request.getEndTime() != null, OrdOrder::getCreateTime, request.getEndTime())
                .eq(OrdOrder::getDeleted, false)
                .orderByDesc(OrdOrder::getCreateTime);
        return ordOrderMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
    }

    /**
     * 查询订单的所有明细行。
     */
    @Slave
    public List<OrdOrderItem> listItemsByOrderId(UUID orderId) {
        if (orderId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<OrdOrderItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrderItem::getOrderId, orderId);
        return ordOrderItemMapper.selectList(wrapper);
    }
}

