package com.cognora.scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.status.StatusValidator;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.scm.finance.domain.entity.SettlementOrder;
import com.cognora.scm.finance.domain.support.FinanceAmount;
import com.cognora.scm.finance.mapper.SettlementOrderMapper;
import com.cognora.scm.finance.service.ISettlementOrderService;
import com.cognora.scm.finance.service.support.AccountingPeriodGuard;
import com.cognora.scm.finance.service.support.FinanceTenantGuard;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class SettlementOrderServiceImpl extends ServiceImpl<SettlementOrderMapper, SettlementOrder>
        implements ISettlementOrderService {
    private final StatusValidator statusValidator;
    private final AccountingPeriodGuard accountingPeriodGuard;

    public SettlementOrderServiceImpl(StatusValidator statusValidator,
                                      AccountingPeriodGuard accountingPeriodGuard) {
        this.statusValidator = statusValidator;
        this.accountingPeriodGuard = accountingPeriodGuard;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder createSettlement(SettlementOrder order) {
        log.info("Create settlement order: partnerName={}, settlementType={}", (order.getPartnerName() + "").replace("\r", "_").replace("\n", "_"), (order.getSettlementType() + "").replace("\r", "_").replace("\n", "_"));

        // 请求体里带其他租户的 tenantId 时直接 403，而不是静默改写成当前租户。
        FinanceTenantGuard.verifyOwnership(order.getTenantId());
        order.setId(UUIDv7Util.generateString());
        order.setSettlementNo(generateSettlementNo());
        order.setStatus(0);
        order.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        order.setCurrency(FinanceAmount.currency(order.getCurrency()));
        normalizeAmounts(order);
        accountingPeriodGuard.assertWritable(order.getSettlementPeriod(), order.getStartDate(),
                "结算单:" + order.getSettlementNo());
        order.setPaidAmount(new BigDecimal("0.00"));
        order.setDeleted(false);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        boolean success = save(order);
        if (!success) {
            throw new RuntimeException("Failed to create settlement order");
        }

        log.info("Settlement order created successfully: id={}, settlementNo={}", (order.getId() + "").replace("\r", "_").replace("\n", "_"), (order.getSettlementNo() + "").replace("\r", "_").replace("\n", "_"));
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder confirmSettlement(String id, String approverId, String approverName) {
        log.info("Confirm settlement: id={}, approver={}", (id + "").replace("\r", "_").replace("\n", "_"), (approverName + "").replace("\r", "_").replace("\n", "_"));

        SettlementOrder order = getById(id);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw new IllegalArgumentException("缁撶畻鍗曚笉瀛樺湪: " + id);
        }
        FinanceTenantGuard.verifyOwnership(order.getTenantId());
        accountingPeriodGuard.assertWritable(order.getSettlementPeriod(), order.getStartDate(),
                "结算单:" + order.getSettlementNo());
        statusValidator.validateTransition("SETTLEMENT", "DRAFT", "CONFIRMED");

        order.setStatus(1);
        order.setApproverId(approverId);
        order.setApproverName(approverName);
        order.setApprovedAt(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        updateById(order);
        log.info("Settlement order confirmed successfully: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder recordPayment(String id, BigDecimal amount) {
        log.info("Record settlement payment: id={}, amount={}", (id + "").replace("\r", "_").replace("\n", "_"), (amount + "").replace("\r", "_").replace("\n", "_"));

        BigDecimal payment = FinanceAmount.positive(amount, "付款金额");

        SettlementOrder order = getById(id);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw new IllegalArgumentException("缁撶畻鍗曚笉瀛樺湪: " + id);
        }
        FinanceTenantGuard.verifyOwnership(order.getTenantId());
        accountingPeriodGuard.assertWritable(order.getSettlementPeriod(), order.getStartDate(),
                "结算单:" + order.getSettlementNo());
        BigDecimal paid = FinanceAmount.optionalOrZero(order.getPaidAmount(), "已付金额");
        BigDecimal payable = FinanceAmount.required(order.getActualAmount(), "应付金额");
        BigDecimal newPaidAmount = FinanceAmount.normalize(paid.add(payment), "累计已付金额");
        if (newPaidAmount.compareTo(payable) > 0) {
            throw new IllegalArgumentException(
                    String.format("Payment exceeds payable amount: paid=%s, current=%s, payable=%s",
                            order.getPaidAmount(), payment, order.getActualAmount()));
        }

        order.setPaidAmount(newPaidAmount);
        order.setUnpaidAmount(FinanceAmount.normalize(payable.subtract(newPaidAmount), "未付金额"));
        order.setUpdateTime(LocalDateTime.now());

        String targetStatus = order.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0 ? "FULLY_PAID" : "PARTIAL_PAID";
        statusValidator.validateTransition("SETTLEMENT", resolveSettlementStatusName(order.getStatus()), targetStatus);

        if (order.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(4);
            log.info("Settlement order fully paid: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        } else {
            order.setStatus(3);
            log.info("Settlement order partially paid: id={}, paid={}, unpaid={}", (id + "").replace("\r", "_").replace("\n", "_"), (newPaidAmount + "").replace("\r", "_").replace("\n", "_"), (order.getUnpaidAmount() + "").replace("\r", "_").replace("\n", "_"));
        }

        updateById(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder updateSettlement(String id, SettlementOrder order) {
        log.info("Update settlement order: id={}", (id + "").replace("\r", "_").replace("\n", "_"));

        SettlementOrder existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new IllegalArgumentException("结算单不存在: " + id);
        }
        FinanceTenantGuard.verifyOwnership(existing.getTenantId());
        accountingPeriodGuard.assertWritable(existing.getSettlementPeriod(), existing.getStartDate(),
                "结算单:" + existing.getSettlementNo());

        // 金额、状态、租户与逻辑删除标记由服务端维护，请求体一律不得改写。
        rejectIfChanged("状态", existing.getStatus(), order.getStatus());
        rejectIfChanged("结算总金额", existing.getTotalAmount(), order.getTotalAmount());
        rejectIfChanged("折扣金额", existing.getDiscountAmount(), order.getDiscountAmount());
        rejectIfChanged("调整金额", existing.getAdjustmentAmount(), order.getAdjustmentAmount());
        rejectIfChanged("应付金额", existing.getActualAmount(), order.getActualAmount());
        rejectIfChanged("已付金额", existing.getPaidAmount(), order.getPaidAmount());
        rejectIfChanged("未付金额", existing.getUnpaidAmount(), order.getUnpaidAmount());
        rejectIfChanged("结算单号", existing.getSettlementNo(), order.getSettlementNo());
        rejectIfChanged("租户", existing.getTenantId(), order.getTenantId());
        rejectIfChanged("删除标记", existing.getDeleted(), order.getDeleted());

        boolean hasBusinessChanges = order.getSettlementType() != null
                || order.getPartnerType() != null
                || order.getPartnerId() != null
                || order.getPartnerName() != null
                || order.getSettlementPeriod() != null
                || order.getStartDate() != null
                || order.getEndDate() != null;
        if (existing.getStatus() != 0 && hasBusinessChanges) {
            throw new IllegalStateException("结算单已确认，仅允许修改备注与附件: " + id);
        }
        if (existing.getStatus() == 0) {
            if (order.getSettlementType() != null) existing.setSettlementType(order.getSettlementType());
            if (order.getPartnerType() != null) existing.setPartnerType(order.getPartnerType());
            if (order.getPartnerId() != null) existing.setPartnerId(order.getPartnerId());
            if (order.getPartnerName() != null) existing.setPartnerName(order.getPartnerName());
            if (order.getSettlementPeriod() != null) existing.setSettlementPeriod(order.getSettlementPeriod());
            if (order.getStartDate() != null) existing.setStartDate(order.getStartDate());
            if (order.getEndDate() != null) existing.setEndDate(order.getEndDate());
        }
        if (order.getRemark() != null) existing.setRemark(order.getRemark());
        if (order.getAttachments() != null) existing.setAttachments(order.getAttachments());

        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
        log.info("Settlement order updated successfully: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return existing;
    }

    @Override
    public void deleteSettlement(String id) {
        SettlementOrder order = getById(id);
        if (order != null) {
            FinanceTenantGuard.verifyOwnership(order.getTenantId());
            accountingPeriodGuard.assertWritable(order.getSettlementPeriod(), order.getStartDate(),
                    "结算单:" + order.getSettlementNo());
            order.setDeleted(true);
            order.setUpdateTime(LocalDateTime.now());
            updateById(order);
            log.info("Settlement order deleted successfully: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        }
    }

    @Override
    public Page<SettlementOrder> listByStatus(Integer status, int page, int size) {
        log.debug("Query settlement orders by status: status={}, page={}, size={}", status, page, size);
        LambdaQueryWrapper<SettlementOrder> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(SettlementOrder::getStatus, status);
        }
        wrapper.eq(SettlementOrder::getDeleted, false);
        wrapper.orderByDesc(SettlementOrder::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    private String generateSettlementNo() {
        return "STL" + System.currentTimeMillis();
    }

    /**
     * 创建时把金额统一到两位小数，并校验「应付 = 总额 - 折扣 + 调整」。
     */
    private void normalizeAmounts(SettlementOrder order) {
        BigDecimal total = FinanceAmount.required(order.getTotalAmount(), "结算总金额");
        BigDecimal discount = FinanceAmount.optionalOrZero(order.getDiscountAmount(), "折扣金额");
        BigDecimal adjustment = order.getAdjustmentAmount() == null
                ? new BigDecimal("0.00")
                : FinanceAmount.signed(order.getAdjustmentAmount(), "调整金额");
        BigDecimal expected = FinanceAmount.normalize(total.subtract(discount).add(adjustment), "应付金额");
        if (expected.signum() < 0) {
            throw new IllegalArgumentException("应付金额不能为负数: " + expected.toPlainString());
        }
        BigDecimal actual = FinanceAmount.optional(order.getActualAmount(), "应付金额");
        if (actual == null) {
            actual = expected;
        } else if (actual.compareTo(expected) != 0) {
            throw new IllegalArgumentException(
                    String.format("应付金额与结算金额不一致: actual=%s, total-discount+adjustment=%s",
                            actual.toPlainString(), expected.toPlainString()));
        }
        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setAdjustmentAmount(adjustment);
        order.setActualAmount(actual);
        order.setUnpaidAmount(actual);
    }

    private void rejectIfChanged(String field, Object current, Object requested) {
        if (requested == null || current == null) {
            return;
        }
        if (!current.equals(requested)) {
            throw new IllegalArgumentException(field + "由服务端维护，不允许通过更新接口修改");
        }
    }

    private String resolveSettlementStatusName(int status) {
        return switch (status) {
            case 0 -> "DRAFT";
            case 1 -> "CONFIRMED";
            case 2 -> "PARTIAL_PAID";
            case 3 -> "FULLY_PAID";
            case 4 -> "CLOSED";
            default -> throw new IllegalStateException("鏈煡鐨勭粨绠楀崟鐘舵€? " + status);
        };
    }
}
