package com.scmcloud.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.purchase.domain.entity.PurReceipt;
import com.scmcloud.purchase.mapper.PurReceiptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurReceiptQueryService {

    private final PurReceiptMapper purReceiptMapper;

    @Slave
    public PurReceipt getById(String id) {
        return purReceiptMapper.selectById(id);
    }

    @Slave
    public List<PurReceipt> listAll() {
        return purReceiptMapper.selectList(null);
    }

    @Slave
    public Page<PurReceipt> pageQuery(Page<PurReceipt> page, Wrapper<PurReceipt> wrapper) {
        return purReceiptMapper.selectPage(page, wrapper);
    }

    @Slave
    public PurReceipt getByReceiptNo(String receiptNo) {
        LambdaQueryWrapper<PurReceipt> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurReceipt::getReceiptNo, receiptNo);
        wrapper.eq(PurReceipt::getDeleted, false);
        return purReceiptMapper.selectOne(wrapper);
    }

    @Slave
    public Page<PurReceipt> pageQuery(int page, int size, Integer status, Integer receiptType, String supplierId, String keyword) {
        LambdaQueryWrapper<PurReceipt> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurReceipt::getStatus, status);
        }
        if (receiptType != null) {
            wrapper.eq(PurReceipt::getReceiptType, receiptType);
        }
        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(PurReceipt::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(PurReceipt::getReceiptNo, keyword)
                    .or()
                    .like(PurReceipt::getOrderNo, keyword)
                    .or()
                    .like(PurReceipt::getSupplierName, keyword));
        }
        wrapper.eq(PurReceipt::getDeleted, false);
        wrapper.orderByDesc(PurReceipt::getCreateTime);
        return purReceiptMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<PurReceipt> listByOrderId(String orderId) {
        LambdaQueryWrapper<PurReceipt> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurReceipt::getOrderId, orderId);
        wrapper.eq(PurReceipt::getDeleted, false);
        wrapper.orderByDesc(PurReceipt::getCreateTime);
        return purReceiptMapper.selectList(wrapper);
    }
}
