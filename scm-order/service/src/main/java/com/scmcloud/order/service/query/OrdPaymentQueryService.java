package com.scmcloud.order.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.order.domain.entity.OrdPayment;
import com.scmcloud.order.domain.entity.PaymentStatus;
import com.scmcloud.order.mapper.OrdPaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 支付记录查询服务（{@code @Slave} 路由到只读副本）。
 *
 * @author SCM Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdPaymentQueryService {

    private final OrdPaymentMapper ordPaymentMapper;

    @Slave
    public OrdPayment getById(UUID id) {
        return ordPaymentMapper.selectById(id);
    }

    @Slave
    public List<OrdPayment> listByOrderId(UUID orderId) {
        if (orderId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<OrdPayment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdPayment::getOrderId, orderId)
                .orderByDesc(OrdPayment::getCreateTime);
        return ordPaymentMapper.selectList(wrapper);
    }

    /**
     * 自定义条件分页查询。
     */
    @Slave
    public Page<OrdPayment> pageWithWrapper(Page<OrdPayment> page,
                                             String paymentNo,
                                             UUID orderId,
                                             String userId,
                                             PaymentStatus status,
                                             LocalDateTime startTime,
                                             LocalDateTime endTime) {
        LambdaQueryWrapper<OrdPayment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(paymentNo != null && !paymentNo.isBlank(), OrdPayment::getPaymentNo, paymentNo)
                .eq(orderId != null, OrdPayment::getOrderId, orderId)
                .eq(userId != null && !userId.isBlank(), OrdPayment::getUserId, userId)
                .eq(status != null, OrdPayment::getStatus, status.getCode())
                .ge(startTime != null, OrdPayment::getInitiatedAt, startTime)
                .le(endTime != null, OrdPayment::getInitiatedAt, endTime)
                .orderByDesc(OrdPayment::getCreateTime);
        return ordPaymentMapper.selectPage(page, wrapper);
    }
}
