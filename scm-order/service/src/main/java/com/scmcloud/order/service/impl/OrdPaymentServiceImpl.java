package com.scmcloud.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.order.domain.entity.OrdPayment;
import com.scmcloud.order.domain.entity.PaymentStatus;
import com.scmcloud.order.mapper.OrdPaymentMapper;
import com.scmcloud.order.service.IOrdPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class OrdPaymentServiceImpl extends ServiceImpl<OrdPaymentMapper, OrdPayment> implements IOrdPaymentService {

    @Autowired
    private StatusValidator statusValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrdPayment createPayment(OrdPayment payment) {
        log.info("创建支付记录: orderNo={}, amount={}", payment.getOrderNo(), payment.getPaymentAmount());

        payment.setStatusEnum(PaymentStatus.PENDING);
        payment.setInitiatedAt(LocalDateTime.now());
        payment.setCreateTime(LocalDateTime.now());
        payment.setUpdateTime(LocalDateTime.now());

        boolean saved = save(payment);
        if (!saved) {
            throw new RuntimeException("创建支付记录失败");
        }

        log.info("支付记录创建成功: id={}, paymentNo={}", payment.getId(), payment.getPaymentNo());
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePaymentStatus(UUID paymentId, Integer status) {
        PaymentStatus target = PaymentStatus.fromCode(status);
        log.info("更新支付状态 paymentId={}, target={}", paymentId, target);

        OrdPayment payment = getById(paymentId);
        if (payment == null) {
            log.warn("支付记录不存在 paymentId={}", paymentId);
            return false;
        }

        String fromName = payment.getStatusEnum().name();
        String toName = target.name();
        statusValidator.validateTransition("PAYMENT", fromName, toName);

        payment.setStatusEnum(target);
        payment.setUpdateTime(LocalDateTime.now());

        switch (target) {
            case SUCCESS -> payment.setPaidAt(LocalDateTime.now());
            case FAILED -> payment.setFailedAt(LocalDateTime.now());
            case REFUNDED -> payment.setRefundedAt(LocalDateTime.now());
            default -> { /* 其他状态不需要时间戳 */ }
        }

        return updateById(payment);
    }
}
