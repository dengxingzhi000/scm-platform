package com.scmcloud.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.payment.domain.entity.Refund;

import java.math.BigDecimal;

public interface IRefundService extends IService<Refund> {

    Refund createRefund(String paymentNo, String orderNo, String userId, BigDecimal amount, String reason);

    Refund queryRefund(String refundNo);
}
