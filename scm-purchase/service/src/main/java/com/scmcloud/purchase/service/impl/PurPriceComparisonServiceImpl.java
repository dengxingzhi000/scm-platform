package com.scmcloud.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.common.status.StatusValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scmcloud.purchase.domain.entity.PurPriceComparison;
import com.scmcloud.purchase.mapper.PurPriceComparisonMapper;
import com.scmcloud.purchase.service.IPurPriceComparisonService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PurPriceComparisonServiceImpl extends ServiceImpl<PurPriceComparisonMapper, PurPriceComparison> implements IPurPriceComparisonService {

    @Autowired
    private StatusValidator statusValidator;

    @Override
    public PurPriceComparison getByComparisonNo(String comparisonNo) {
        return lambdaQuery()
                .eq(PurPriceComparison::getComparisonNo, comparisonNo)
                .eq(PurPriceComparison::getDeleted, false)
                .one();
    }

    @Override
    public Page<PurPriceComparison> pageQuery(int page, int size, Integer status, String rfqId) {
        LambdaQueryWrapper<PurPriceComparison> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurPriceComparison::getStatus, status);
        }
        if (StringUtils.hasText(rfqId)) {
            wrapper.eq(PurPriceComparison::getRfqId, rfqId);
        }
        wrapper.eq(PurPriceComparison::getDeleted, false);
        wrapper.orderByDesc(PurPriceComparison::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<PurPriceComparison> listByRfqId(String rfqId) {
        return lambdaQuery()
                .eq(PurPriceComparison::getRfqId, rfqId)
                .eq(PurPriceComparison::getDeleted, false)
                .orderByDesc(PurPriceComparison::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(String id, String approverId, String approverName) {
        PurPriceComparison comparison = getById(id);
        if (comparison == null || comparison.getDeleted()) {
            throw new IllegalArgumentException("Price comparison not found: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "PENDING_APPROVAL", "APPROVED");
        comparison.setStatus(2); // APPROVED
        comparison.setApprovedBy(approverId);
        comparison.setApprovedByName(approverName);
        comparison.setApprovedAt(LocalDateTime.now());
        comparison.setUpdateTime(LocalDateTime.now());
        return updateById(comparison);
    }
}
