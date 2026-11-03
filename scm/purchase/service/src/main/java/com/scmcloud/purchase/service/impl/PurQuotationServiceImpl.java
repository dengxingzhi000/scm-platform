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
import com.scmcloud.purchase.domain.entity.PurQuotation;
import com.scmcloud.purchase.mapper.PurQuotationMapper;
import com.scmcloud.purchase.service.IPurQuotationService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PurQuotationServiceImpl extends ServiceImpl<PurQuotationMapper, PurQuotation> implements IPurQuotationService {

    @Autowired
    private StatusValidator statusValidator;

    @Override
    public PurQuotation getByQuotationNo(String quotationNo) {
        return lambdaQuery()
                .eq(PurQuotation::getQuotationNo, quotationNo)
                .eq(PurQuotation::getDeleted, false)
                .one();
    }

    @Override
    public Page<PurQuotation> pageQuery(int page, int size, Integer status, String supplierId, String rfqId) {
        LambdaQueryWrapper<PurQuotation> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurQuotation::getStatus, status);
        }
        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(PurQuotation::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(rfqId)) {
            wrapper.eq(PurQuotation::getRfqId, rfqId);
        }
        wrapper.eq(PurQuotation::getDeleted, false);
        wrapper.orderByDesc(PurQuotation::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<PurQuotation> listByRfqId(String rfqId) {
        return lambdaQuery()
                .eq(PurQuotation::getRfqId, rfqId)
                .eq(PurQuotation::getDeleted, false)
                .orderByDesc(PurQuotation::getCreateTime)
                .list();
    }

    @Override
    public List<PurQuotation> listBySupplierId(String supplierId) {
        return lambdaQuery()
                .eq(PurQuotation::getSupplierId, supplierId)
                .eq(PurQuotation::getDeleted, false)
                .orderByDesc(PurQuotation::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(String id) {
        PurQuotation quotation = getById(id);
        if (quotation == null || quotation.getDeleted()) {
            throw new IllegalArgumentException("报价单不存在: " + id);
        }
        statusValidator.validateTransition("QUOTATION", "DRAFT", "SUBMITTED");
        quotation.setStatus(1); // SUBMITTED
        quotation.setUpdateTime(LocalDateTime.now());
        return updateById(quotation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean select(String id, String selectedBy, String selectedByName) {
        PurQuotation quotation = getById(id);
        if (quotation == null || quotation.getDeleted()) {
            throw new IllegalArgumentException("报价单不存在: " + id);
        }
        if (quotation.getStatus() < 1) {
            throw new IllegalStateException("只有已提交的报价单才能被选中");
        }
        quotation.setIsSelected(true);
        quotation.setSelectedBy(selectedBy);
        quotation.setSelectedByName(selectedByName);
        quotation.setSelectedAt(LocalDateTime.now());
        quotation.setUpdateTime(LocalDateTime.now());
        return updateById(quotation);
    }
}
