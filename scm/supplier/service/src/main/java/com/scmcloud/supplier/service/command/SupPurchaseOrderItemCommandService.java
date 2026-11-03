package com.scmcloud.supplier.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.supplier.domain.entity.SupPurchaseOrderItem;
import com.scmcloud.supplier.mapper.SupPurchaseOrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupPurchaseOrderItemCommandService {

    private final SupPurchaseOrderItemMapper supPurchaseOrderItemMapper;

    @Master(reason = "保存采购单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SupPurchaseOrderItem entity) {
        return supPurchaseOrderItemMapper.insert(entity) > 0;
    }

    @Master(reason = "更新采购单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SupPurchaseOrderItem entity) {
        return supPurchaseOrderItemMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除采购单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return supPurchaseOrderItemMapper.deleteById(id) > 0;
    }
}
