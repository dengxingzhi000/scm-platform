package com.scmcloud.finance.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.finance.domain.entity.SettlementOrder;
import com.scmcloud.finance.mapper.SettlementOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementOrderCommandService {
    private final StatusValidator statusValidator;
    private final SettlementOrderMapper settlementOrderMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder createSettlement(SettlementOrder order) {
        log.info("创建结算单: partnerName={}, settlementType={}", order.getPartnerName(), order.getSettlementType());
        order.setId(UUIDv7Util.generateString());
        order.setSettlementNo(generateSettlementNo());
        order.setStatus(0);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setUnpaidAmount(order.getActualAmount());
        order.setDeleted(false);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        int rows = settlementOrderMapper.insert(order);
        if (rows <= 0) {
            throw new RuntimeException("创建结算单失败");
        }
        log.info("结算单创建成功: id={}, settlementNo={}", order.getId(), order.getSettlementNo());
        return order;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder confirmSettlement(String id, String approverId, String approverName) {
        log.info("确认结算单: id={}, approver={}", id, approverName);
        SettlementOrder order = settlementOrderMapper.selectById(id);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw new IllegalArgumentException("结算单不存在: " + id);
        }
        statusValidator.validateTransition("SETTLEMENT", "DRAFT", "CONFIRMED");
        order.setStatus(1);
        order.setApproverId(approverId);
        order.setApproverName(approverName);
        order.setApprovedAt(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        settlementOrderMapper.updateById(order);
        log.info("结算单确认成功: id={}", id);
        return order;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder recordPayment(String id, BigDecimal amount) {
        log.info("记录结算单付款: id={}, amount={}", id, amount);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("付款金额必须大于0");
        }
        SettlementOrder order = settlementOrderMapper.selectById(id);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw new IllegalArgumentException("结算单不存在: " + id);
        }
        BigDecimal newPaidAmount = order.getPaidAmount().add(amount);
        if (newPaidAmount.compareTo(order.getActualAmount()) > 0) {
            throw new IllegalArgumentException(
                    String.format("付款金额超出应付金额: 已付=%s, 本次=%s, 应付=%s",
                            order.getPaidAmount(), amount, order.getActualAmount()));
        }
        order.setPaidAmount(newPaidAmount);
        order.setUnpaidAmount(order.getActualAmount().subtract(newPaidAmount));
        order.setUpdateTime(LocalDateTime.now());
        String targetStatus = order.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0 ? "FULLY_PAID" : "PARTIAL_PAID";
        statusValidator.validateTransition("SETTLEMENT", resolveSettlementStatusName(order.getStatus()), targetStatus);
        if (order.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(4);
        } else {
            order.setStatus(3);
        }
        settlementOrderMapper.updateById(order);
        return order;
    }

    private String generateSettlementNo() {
        return "STL" + System.currentTimeMillis();
    }

    private String resolveSettlementStatusName(int status) {
        return switch (status) {
            case 0 -> "DRAFT";
            case 1 -> "CONFIRMED";
            case 2 -> "PARTIAL_PAID";
            case 3 -> "FULLY_PAID";
            case 4 -> "CLOSED";
            default -> throw new IllegalStateException("未知的结算单状态: " + status);
        };
    }
}
