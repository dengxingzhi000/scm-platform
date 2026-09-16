package com.scmcloud.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.status.StatusValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scmcloud.purchase.domain.entity.PurReceipt;
import com.scmcloud.purchase.mapper.PurReceiptMapper;
import com.scmcloud.purchase.service.IPurReceiptService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PurReceiptServiceImpl extends ServiceImpl<PurReceiptMapper, PurReceipt> implements IPurReceiptService {

    @Autowired
    private StatusValidator statusValidator;

    @Override
    public PurReceipt getByReceiptNo(String receiptNo) {
        return lambdaQuery()
                .eq(PurReceipt::getReceiptNo, receiptNo)
                .eq(PurReceipt::getDeleted, false)
                .one();
    }

    @Override
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
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<PurReceipt> listByOrderId(String orderId) {
        return lambdaQuery()
                .eq(PurReceipt::getOrderId, orderId)
                .eq(PurReceipt::getDeleted, false)
                .orderByDesc(PurReceipt::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean receive(String id, String receiverId, String receiverName) {
        PurReceipt receipt = getById(id);
        if (receipt == null || receipt.getDeleted()) {
            throw new IllegalArgumentException("入库单不存在: " + id);
        }
        statusValidator.validateTransition("RECEIPT", "WAITING", "RECEIVING");
        receipt.setStatus(1); // RECEIVING
        receipt.setReceiverId(receiverId);
        receipt.setReceiverName(receiverName);
        receipt.setReceivedAt(LocalDateTime.now());
        receipt.setUpdateTime(LocalDateTime.now());
        return updateById(receipt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean qualityInspect(String id, String inspectorId, String inspectorName, Integer result, String remark) {
        PurReceipt receipt = getById(id);
        if (receipt == null || receipt.getDeleted()) {
            throw new IllegalArgumentException("入库单不存在: " + id);
        }
        statusValidator.validateTransition("RECEIPT", "RECEIVING", "INSPECTING");
        receipt.setStatus(2); // INSPECTING
        receipt.setQualityInspectorId(inspectorId);
        receipt.setQualityInspectorName(inspectorName);
        receipt.setQualityInspectedAt(LocalDateTime.now());
        receipt.setQualityResult(result);
        receipt.setQualityRemark(remark);
        receipt.setUpdateTime(LocalDateTime.now());
        return updateById(receipt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean shelve(String id, String shelvedBy, String shelvedByName) {
        PurReceipt receipt = getById(id);
        if (receipt == null || receipt.getDeleted()) {
            throw new IllegalArgumentException("入库单不存在: " + id);
        }
        statusValidator.validateTransition("RECEIPT", "INSPECTING", "COMPLETED");
        receipt.setStatus(3); // COMPLETED
        receipt.setShelved(true);
        receipt.setShelvedBy(shelvedBy);
        receipt.setShelvedByName(shelvedByName);
        receipt.setShelvedAt(LocalDateTime.now());
        receipt.setUpdateTime(LocalDateTime.now());
        return updateById(receipt);
    }
}
