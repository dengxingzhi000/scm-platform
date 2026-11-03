package com.cognora.scm.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.scm.supplier.domain.entity.SupPurchaseOrderItem;
import com.cognora.scm.supplier.mapper.SupPurchaseOrderItemMapper;
import com.cognora.scm.supplier.service.ISupPurchaseOrderItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SupPurchaseOrderItemServiceImpl
        extends ServiceImpl<SupPurchaseOrderItemMapper, SupPurchaseOrderItem>
        implements ISupPurchaseOrderItemService {

    @Override
    public Page<SupPurchaseOrderItem> pageList(int page, int size, String purchaseId, String skuId) {
        log.debug("分页查询采购单明细 page={}, size={}, purchaseId={}, skuId={}", (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (purchaseId + "").replace("\r", "_").replace("\n", "_"), (skuId + "").replace("\r", "_").replace("\n", "_"));

        LambdaQueryWrapper<SupPurchaseOrderItem> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(purchaseId)) {
            wrapper.eq(SupPurchaseOrderItem::getPurchaseId, purchaseId);
        }
        if (StringUtils.hasText(skuId)) {
            wrapper.eq(SupPurchaseOrderItem::getSkuId, skuId);
        }

        wrapper.orderByDesc(SupPurchaseOrderItem::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<SupPurchaseOrderItem> listByPurchaseId(String purchaseId) {
        if (!StringUtils.hasText(purchaseId)) {
            return List.of();
        }
        log.debug("查询采购单的所有明细 purchaseId={}", (purchaseId + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(SupPurchaseOrderItem::getPurchaseId, purchaseId)
                .orderByAsc(SupPurchaseOrderItem::getCreateTime)
                .list();
    }

    @Override
    public SupPurchaseOrderItem createItem(SupPurchaseOrderItem item) {
        log.info("[API] 创建采购单明细 purchaseId={}, skuId={}", (item.getPurchaseId() + "").replace("\r", "_").replace("\n", "_"), (item.getSkuId() + "").replace("\r", "_").replace("\n", "_"));
        item.setId(UUID.randomUUID());
        item.setCreateTime(LocalDateTime.now());
        calculateSubtotal(item);
        save(item);
        log.info("[API] 采购单明细创建成功 id={}", item.getId());
        return item;
    }

    @Override
    public SupPurchaseOrderItem updateItem(UUID id, SupPurchaseOrderItem item) {
        log.info("[API] 更新采购单明细 id={}", id);
        SupPurchaseOrderItem existing = getById(id);
        if (existing == null) {
            return null;
        }
        item.setId(id);
        calculateSubtotal(item);
        updateById(item);
        return getById(id);
    }

    private void calculateSubtotal(SupPurchaseOrderItem item) {
        if (item.getSubtotal() == null && item.getUnitPrice() != null && item.getQuantity() != null) {
            item.setSubtotal(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
        }
    }
}
