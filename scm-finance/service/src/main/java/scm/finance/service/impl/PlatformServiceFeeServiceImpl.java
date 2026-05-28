package scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import scm.finance.domain.entity.PlatformServiceFee;
import scm.finance.mapper.PlatformServiceFeeMapper;
import scm.finance.service.IPlatformServiceFeeService;
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
        log.debug("查询待付款平台服务费");
        LambdaQueryWrapper<PlatformServiceFee> wrapper = Wrappers.lambdaQuery()
                .eq(PlatformServiceFee::getStatus, 0)
                .orderByDesc(PlatformServiceFee::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformServiceFee recordPayment(String id, BigDecimal paidAmount) {
        log.info("记录平台服务费付款: id={}, paidAmount={}", id, paidAmount);

        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("付款金额必须大于0");
        }

        PlatformServiceFee fee = getById(id);
        if (fee == null) {
            throw new IllegalArgumentException("平台服务费记录不存在: " + id);
        }
        if (fee.getStatus() != 0) {
            throw new IllegalStateException("只有待付款状态的记录才能付款, 当前状态: " + fee.getStatus());
        }

        BigDecimal finalFee = fee.getFinalFee() != null ? fee.getFinalFee() : fee.getTotalFee();
        if (paidAmount.compareTo(finalFee) != 0) {
            throw new IllegalArgumentException(
                    String.format("付款金额与应付金额不一致: paid=%s, final=%s", paidAmount, finalFee));
        }

        fee.setPaidAmount(paidAmount);
        fee.setPaidAt(LocalDateTime.now());
        fee.setStatus(1);
        fee.setUpdateTime(LocalDateTime.now());

        updateById(fee);
        log.info("平台服务费付款成功: id={}", id);
        return fee;
    }
}
