package com.scmcloud.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.finance.domain.entity.PlatformServiceFee;
import com.scmcloud.finance.mapper.PlatformServiceFeeMapper;
import com.scmcloud.finance.service.IPlatformServiceFeeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
    public List<PlatformServiceFee> listPendingFees() {
        log.debug("鏌ヨ寰呬粯娆惧钩鍙版湇鍔¤垂");
        LambdaQueryWrapper<PlatformServiceFee> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PlatformServiceFee::getStatus, 0)
                .orderByDesc(PlatformServiceFee::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformServiceFee recordPayment(String id, BigDecimal paidAmount) {
        log.info("Record platform service fee payment: id={}, paidAmount={}", id, paidAmount);

        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        PlatformServiceFee fee = getById(id);
        if (fee == null) {
            throw new IllegalArgumentException("Platform service fee record not found: " + id);
        }
        if (fee.getStatus() != 0) {
            throw new IllegalStateException("Only pending payment records can be paid, current status: " + fee.getStatus());
        }

        BigDecimal finalFee = fee.getFinalFee() != null ? fee.getFinalFee() : fee.getTotalFee();
        if (paidAmount.compareTo(finalFee) != 0) {
            throw new IllegalArgumentException(
                    String.format("Payment amount does not match payable amount: paid=%s, final=%s", paidAmount, finalFee));
        }

        fee.setPaidAmount(paidAmount);
        fee.setPaidAt(LocalDateTime.now());
        fee.setStatus(1);
        fee.setUpdateTime(LocalDateTime.now());

        updateById(fee);
        log.info("Platform service fee payment successful: id={}", id);
        return fee;
    }
}
