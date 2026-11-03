package com.scmcloud.finance.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.finance.domain.entity.PlatformServiceFee;
import com.scmcloud.finance.mapper.PlatformServiceFeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformServiceFeeCommandService {
    private final StatusValidator statusValidator;
    private final PlatformServiceFeeMapper platformServiceFeeMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public PlatformServiceFee recordPayment(String id, BigDecimal paidAmount) {
        log.info("记录平台服务费付款: id={}, paidAmount={}", id, paidAmount);

        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("付款金额必须大于0");
        }

        PlatformServiceFee fee = platformServiceFeeMapper.selectById(id);
        if (fee == null) {
            throw new IllegalArgumentException("平台服务费记录不存在: " + id);
        }
        statusValidator.validateTransition("SETTLEMENT", "DRAFT", "CONFIRMED");

        BigDecimal finalFee = fee.getFinalFee() != null ? fee.getFinalFee() : fee.getTotalFee();
        if (paidAmount.compareTo(finalFee) != 0) {
            throw new IllegalArgumentException(
                    String.format("付款金额与应付金额不一致: paid=%s, final=%s", paidAmount, finalFee));
        }

        fee.setPaidAmount(paidAmount);
        fee.setPaidAt(LocalDateTime.now());
        fee.setStatus(1);
        fee.setUpdateTime(LocalDateTime.now());

        platformServiceFeeMapper.updateById(fee);
        log.info("平台服务费付款成功: id={}", id);
        return fee;
    }
}
