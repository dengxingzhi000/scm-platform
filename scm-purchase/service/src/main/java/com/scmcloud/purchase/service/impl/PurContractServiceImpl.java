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
import com.scmcloud.purchase.domain.entity.PurContract;
import com.scmcloud.purchase.mapper.PurContractMapper;
import com.scmcloud.purchase.service.IPurContractService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PurContractServiceImpl extends ServiceImpl<PurContractMapper, PurContract> implements IPurContractService {

    @Autowired
    private StatusValidator statusValidator;

    @Override
    public PurContract getByContractNo(String contractNo) {
        return lambdaQuery()
                .eq(PurContract::getContractNo, contractNo)
                .eq(PurContract::getDeleted, false)
                .one();
    }

    @Override
    public Page<PurContract> pageQuery(int page, int size, Integer status, Integer contractType, String supplierId, String keyword) {
        LambdaQueryWrapper<PurContract> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurContract::getStatus, status);
        }
        if (contractType != null) {
            wrapper.eq(PurContract::getContractType, contractType);
        }
        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(PurContract::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(PurContract::getContractNo, keyword)
                    .or()
                    .like(PurContract::getContractName, keyword)
                    .or()
                    .like(PurContract::getSupplierName, keyword));
        }
        wrapper.eq(PurContract::getDeleted, false);
        wrapper.orderByDesc(PurContract::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<PurContract> listByStatus(Integer status) {
        return lambdaQuery()
                .eq(PurContract::getStatus, status)
                .eq(PurContract::getDeleted, false)
                .orderByDesc(PurContract::getCreateTime)
                .list();
    }

    @Override
    public List<PurContract> listBySupplierId(String supplierId) {
        return lambdaQuery()
                .eq(PurContract::getSupplierId, supplierId)
                .eq(PurContract::getDeleted, false)
                .orderByDesc(PurContract::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sign(String id, String signedBy, String signedByName) {
        PurContract contract = getById(id);
        if (contract == null || contract.getDeleted()) {
            throw new IllegalArgumentException("Contract not found: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "PENDING_APPROVAL", "APPROVED");
        contract.setStatus(2); // APPROVED
        contract.setSignedBy(signedBy);
        contract.setSignedByName(signedByName);
        contract.setSignedAt(LocalDateTime.now());
        contract.setUpdateTime(LocalDateTime.now());
        return updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean terminate(String id) {
        PurContract contract = getById(id);
        if (contract == null || contract.getDeleted()) {
            throw new IllegalArgumentException("Contract not found: " + id);
        }
        String fromStatus;
        switch (contract.getStatus()) {
            case 0: fromStatus = "DRAFT"; break;
            case 1: fromStatus = "PENDING_APPROVAL"; break;
            case 2: fromStatus = "APPROVED"; break;
            case 3: fromStatus = "REJECTED"; break;
            case 4: fromStatus = "CANCELLED"; break;
            default: throw new IllegalStateException("鏈煡鐘舵€? " + contract.getStatus());
        }
        statusValidator.validateTransition("PURCHASE", fromStatus, "CANCELLED");
        contract.setStatus(4); // CANCELLED
        contract.setUpdateTime(LocalDateTime.now());
        return updateById(contract);
    }
}
