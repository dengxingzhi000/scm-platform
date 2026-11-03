package com.cognora.scm.order.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.scm.order.domain.entity.OrdOrderItem;
import com.cognora.scm.order.dto.OrderConverter;
import com.cognora.scm.order.dto.OrderItemResponse;
import com.cognora.scm.order.dto.OrdOrderItemQueryRequest;
import com.cognora.scm.order.mapper.OrdOrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdOrderItemQueryService {
    private final OrdOrderItemMapper ordOrderItemMapper;

    @Slave
    public OrdOrderItem getById(String id) {
        return ordOrderItemMapper.selectById(id);
    }

    @Slave
    public List<OrdOrderItem> listByOrderId(UUID orderId) {
        LambdaQueryWrapper<OrdOrderItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrderItem::getOrderId, orderId)
                .orderByAsc(OrdOrderItem::getCreateTime);
        return ordOrderItemMapper.selectList(wrapper);
    }

    @Slave
    public Page<OrdOrderItem> pageByOrderId(Page<OrdOrderItem> page, UUID orderId) {
        LambdaQueryWrapper<OrdOrderItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrderItem::getOrderId, orderId)
                .orderByAsc(OrdOrderItem::getCreateTime);
        return ordOrderItemMapper.selectPage(page, wrapper);
    }

    @Slave
    public OrderItemResponse getResponseById(String id) {
        return OrderConverter.toItemResponse(getById(id));
    }

    @Slave
    public Page<OrderItemResponse> pageResponses(OrdOrderItemQueryRequest request) {
        Page<OrdOrderItem> page = pageByOrderId(
                new Page<>(request.getPageNum(), request.getPageSize()), request.getOrderId());
        Page<OrderItemResponse> mapped = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        mapped.setRecords(OrderConverter.toItemResponseList(page.getRecords()));
        return mapped;
    }
}
