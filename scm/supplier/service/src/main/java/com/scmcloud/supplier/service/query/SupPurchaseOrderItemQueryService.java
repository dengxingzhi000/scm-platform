package com.scmcloud.supplier.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.supplier.domain.entity.SupPurchaseOrderItem;
import com.scmcloud.supplier.mapper.SupPurchaseOrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupPurchaseOrderItemQueryService {

    private final SupPurchaseOrderItemMapper supPurchaseOrderItemMapper;

    @Slave
    public SupPurchaseOrderItem getById(String id) {
        return supPurchaseOrderItemMapper.selectById(id);
    }

    @Slave
    public List<SupPurchaseOrderItem> listAll() {
        return supPurchaseOrderItemMapper.selectList(null);
    }

    @Slave
    public Page<SupPurchaseOrderItem> pageQuery(Page<SupPurchaseOrderItem> page, Wrapper<SupPurchaseOrderItem> wrapper) {
        return supPurchaseOrderItemMapper.selectPage(page, wrapper);
    }

    @Slave
    public Page<SupPurchaseOrderItem> pageList(int page, int size, String purchaseId, String skuId) {
        log.debug("分页查询采购单明细: page={}, size={}, purchaseId={}, skuId={}", page, size, purchaseId, skuId);
        LambdaQueryWrapper<SupPurchaseOrderItem> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(purchaseId)) {
            wrapper.eq(SupPurchaseOrderItem::getPurchaseId, purchaseId);
        }
        if (StringUtils.hasText(skuId)) {
            wrapper.eq(SupPurchaseOrderItem::getSkuId, skuId);
        }
        wrapper.orderByDesc(SupPurchaseOrderItem::getCreateTime);
        return supPurchaseOrderItemMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<SupPurchaseOrderItem> listByPurchaseId(String purchaseId) {
        if (!StringUtils.hasText(purchaseId)) {
            return List.of();
        }
        log.debug("查询采购单的所有明细: purchaseId={}", purchaseId);
        LambdaQueryWrapper<SupPurchaseOrderItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SupPurchaseOrderItem::getPurchaseId, purchaseId);
        wrapper.orderByAsc(SupPurchaseOrderItem::getCreateTime);
        return supPurchaseOrderItemMapper.selectList(wrapper);
    }
}
