package com.cognora.scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.scm.finance.domain.entity.PlatformServiceFee;
import com.cognora.scm.finance.domain.support.FinanceAmount;
import com.cognora.scm.finance.mapper.PlatformServiceFeeMapper;
import com.cognora.scm.finance.service.IPlatformServiceFeeService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PlatformServiceFeeServiceImpl extends ServiceImpl<PlatformServiceFeeMapper, PlatformServiceFee>
        implements IPlatformServiceFeeService {

    @Override
    public PlatformServiceFee createFee(PlatformServiceFee fee) {
        fee.setId(UUIDv7Util.generateString());
        fee.setStatus(0);
        fee.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        normalizeAmounts(fee);
        fee.setCreateTime(LocalDateTime.now());
        fee.setUpdateTime(LocalDateTime.now());
        save(fee);
        log.info("Platform service fee created successfully: id={}, feeType={}", (fee.getId() + "").replace("\r", "_").replace("\n", "_"), (fee.getFeeType() + "").replace("\r", "_").replace("\n", "_"));
        return fee;
    }

    @Override
    public PlatformServiceFee updateFee(String id, PlatformServiceFee fee) {
        fee.setId(id);
        fee.setUpdateTime(LocalDateTime.now());
        updateById(fee);
        log.info("Platform service fee updated successfully: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return fee;
    }

    @Override
    public List<PlatformServiceFee> listPendingFees() {

        LambdaQueryWrapper<PlatformServiceFee> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PlatformServiceFee::getStatus, 0)
                .orderByDesc(PlatformServiceFee::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformServiceFee recordPayment(String id, BigDecimal paidAmount) {
        log.info("Record platform service fee payment: id={}, paidAmount={}", (id + "").replace("\r", "_").replace("\n", "_"), (paidAmount + "").replace("\r", "_").replace("\n", "_"));

        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }
        BigDecimal payment = FinanceAmount.positive(paidAmount, "实付金额");

        PlatformServiceFee fee = getById(id);
        if (fee == null) {
            throw new IllegalArgumentException("Platform service fee record not found: " + id);
        }
        if (fee.getStatus() != 0) {
            throw new IllegalStateException("Only pending payment records can be paid, current status: " + fee.getStatus());
        }

        BigDecimal finalFee = FinanceAmount.required(
                fee.getFinalFee() != null ? fee.getFinalFee() : fee.getTotalFee(), "应付服务费");
        if (payment.compareTo(finalFee) != 0) {
            throw new IllegalArgumentException(
                    String.format("Payment amount does not match payable amount: paid=%s, final=%s", payment, finalFee));
        }

        fee.setPaidAmount(payment);
        fee.setPaidAt(LocalDateTime.now());
        fee.setStatus(1);
        fee.setUpdateTime(LocalDateTime.now());

        updateById(fee);
        log.info("Platform service fee payment successful: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return fee;
    }

    /**
     * 服务费金额统一到两位小数；final_fee 为空时按「总额 - 优惠」计算，
     * 两个列在库里都是 NOT NULL。
     */
    private void normalizeAmounts(PlatformServiceFee fee) {
        fee.setTransactionAmount(FinanceAmount.optionalOrZero(fee.getTransactionAmount(), "交易金额"));
        fee.setUnitPrice(FinanceAmount.optional(fee.getUnitPrice(), "单价"));
        fee.setBaseFee(FinanceAmount.optionalOrZero(fee.getBaseFee(), "基础费用"));
        fee.setTransactionFee(FinanceAmount.optionalOrZero(fee.getTransactionFee(), "交易费用"));
        fee.setStorageFee(FinanceAmount.optionalOrZero(fee.getStorageFee(), "存储费用"));
        fee.setApiFee(FinanceAmount.optionalOrZero(fee.getApiFee(), "API费用"));
        BigDecimal total = FinanceAmount.required(fee.getTotalFee(), "费用总额");
        BigDecimal discount = FinanceAmount.optionalOrZero(fee.getDiscountAmount(), "优惠金额");
        BigDecimal finalFee = FinanceAmount.optional(fee.getFinalFee(), "应付金额");
        if (finalFee == null) {
            finalFee = FinanceAmount.nonNegative(total.subtract(discount), "应付金额");
        }
        if (finalFee.compareTo(FinanceAmount.nonNegative(total.subtract(discount), "应付金额")) != 0) {
            throw new IllegalArgumentException("应付金额与「总额 - 优惠」不一致: final="
                    + finalFee.toPlainString());
        }
        fee.setTotalFee(total);
        fee.setDiscountAmount(discount);
        fee.setFinalFee(finalFee);
        fee.setFeeRate(FinanceAmount.rate(fee.getFeeRate(), "费率"));
    }
}