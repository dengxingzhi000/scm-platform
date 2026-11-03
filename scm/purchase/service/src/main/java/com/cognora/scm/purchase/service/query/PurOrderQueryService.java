package com.cognora.scm.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.scm.purchase.domain.entity.PurOrder;
import com.cognora.scm.purchase.mapper.PurOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 采购订单只读查询。采购单由采购流程写入，前端只消费列表与详情。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurOrderQueryService {

    private final PurOrderMapper purOrderMapper;

    @Slave
    public Page<PurOrder> pageList(int page, int size, String keyword, Integer status, String supplierId) {
        log.debug("分页查询采购订单 page={}, size={}, keyword={}, status={}, supplierId={}",
                page, size, keyword, status, supplierId);

        LambdaQueryWrapper<PurOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurOrder::getDeleted, false);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(PurOrder::getOrderNo, keyword)
                    .or().like(PurOrder::getSupplierName, keyword));
        }
        if (status != null) {
            wrapper.eq(PurOrder::getStatus, status);
        }
        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(PurOrder::getSupplierId, supplierId);
        }
        wrapper.orderByDesc(PurOrder::getCreateTime);

        return purOrderMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public PurOrder getById(String id) {
        return purOrderMapper.selectById(id);
    }
}
