package com.scmcloud.purchase.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurContract;
import com.scmcloud.purchase.mapper.PurContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scmcloud.common.status.StatusValidator;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurContractCommandService {

    private final PurContractMapper purContractMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "保存合同")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurContract entity) {
        return purContractMapper.insert(entity) > 0;
    }

    @Master(reason = "更新合同")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurContract entity) {
        return purContractMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除合同")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purContractMapper.deleteById(id) > 0;
    }

    @Master(reason = "签署合同")
    @Transactional(rollbackFor = Exception.class)
    public boolean sign(String id, String signedBy, String signedByName) {
        PurContract contract = purContractMapper.selectById(id);
        if (contract == null || contract.getDeleted()) {
            throw new IllegalArgumentException("合同不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "PENDING_APPROVAL", "APPROVED");
        contract.setStatus(2); // APPROVED
        contract.setSignedBy(signedBy);
        contract.setSignedByName(signedByName);
        contract.setSignedAt(LocalDateTime.now());
        contract.setUpdateTime(LocalDateTime.now());
        return purContractMapper.updateById(contract) > 0;
    }

    @Master(reason = "终止合同")
    @Transactional(rollbackFor = Exception.class)
    public boolean terminate(String id) {
        PurContract contract = purContractMapper.selectById(id);
        if (contract == null || contract.getDeleted()) {
            throw new IllegalArgumentException("合同不存在: " + id);
        }
        String fromStatus;
        switch (contract.getStatus()) {
            case 0: fromStatus = "DRAFT"; break;
            case 1: fromStatus = "PENDING_APPROVAL"; break;
            case 2: fromStatus = "APPROVED"; break;
            case 3: fromStatus = "REJECTED"; break;
            case 4: fromStatus = "CANCELLED"; break;
            default: throw new IllegalStateException("未知状态: " + contract.getStatus());
        }
        statusValidator.validateTransition("PURCHASE", fromStatus, "CANCELLED");
        contract.setStatus(4); // CANCELLED
        contract.setUpdateTime(LocalDateTime.now());
        return purContractMapper.updateById(contract) > 0;
    }
}
