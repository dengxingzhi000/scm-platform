package scm.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import scm.supplier.domain.entity.SupPurchaseOrderItem;
import scm.supplier.mapper.SupPurchaseOrderItemMapper;
import scm.supplier.service.ISupPurchaseOrderItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class SupPurchaseOrderItemServiceImpl
        extends ServiceImpl<SupPurchaseOrderItemMapper, SupPurchaseOrderItem>
        implements ISupPurchaseOrderItemService {

    @Override
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

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<SupPurchaseOrderItem> listByPurchaseId(String purchaseId) {
        if (!StringUtils.hasText(purchaseId)) {
            return List.of();
        }
        log.debug("查询采购单的所有明细: purchaseId={}", purchaseId);
        return lambdaQuery()
                .eq(SupPurchaseOrderItem::getPurchaseId, purchaseId)
                .orderByAsc(SupPurchaseOrderItem::getCreateTime)
                .list();
    }
}
