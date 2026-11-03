package com.scmcloud.supplier.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.supplier.domain.entity.SupSettlement;
import com.scmcloud.supplier.mapper.SupSettlementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSettlementCommandService {

    private final SupSettlementMapper supSettlementMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "保存对账单")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SupSettlement entity) {
        return supSettlementMapper.insert(entity) > 0;
    }

    @Master(reason = "更新对账单")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SupSettlement entity) {
        return supSettlementMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除对账单")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return supSettlementMapper.deleteById(id) > 0;
    }

    @Master(reason = "确认对账单")
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(String id, String approverId, String approverName) {
        log.info("确认对账单: id={}, approverId={}", id, approverId);
        SupSettlement settlement = supSettlementMapper.selectById(id);
        if (settlement == null) {
            log.warn("对账单不存在: id={}", id);
            return false;
        }
        statusValidator.validateTransition("SUPPLIER_SETTLEMENT", "DRAFT", "CONFIRMED");
        settlement.setStatus(1);
        settlement.setApproverId(approverId);
        settlement.setApproverName(approverName);
        settlement.setApprovedAt(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());
        boolean success = supSettlementMapper.updateById(settlement) > 0;
        if (success) {
            log.info("对账单确认成功: id={}", id);
        }
        return success;
    }

    @Master(reason = "标记对账单已付款")
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsPaid(String id, String updateBy) {
        log.info("标记对账单已付款: id={}", id);
        SupSettlement settlement = supSettlementMapper.selectById(id);
        if (settlement == null) {
            log.warn("对账单不存在: id={}", id);
            return false;
        }
        String payFromStatus;
        if (settlement.getStatus() == 1) {
            payFromStatus = "CONFIRMED";
        } else if (settlement.getStatus() == 2) {
            payFromStatus = "PARTIAL_PAID";
        } else {
            payFromStatus = "FULLY_PAID";
        }
        statusValidator.validateTransition("SUPPLIER_SETTLEMENT", payFromStatus, "CLOSED");
        settlement.setStatus(4);
        settlement.setPaymentAmount(settlement.getActualAmount());
        settlement.setUpdateTime(LocalDateTime.now());
        settlement.setUpdateBy(updateBy);
        boolean success = supSettlementMapper.updateById(settlement) > 0;
        if (success) {
            log.info("对账单标记付款成功: id={}", id);
        }
        return success;
    }
}
