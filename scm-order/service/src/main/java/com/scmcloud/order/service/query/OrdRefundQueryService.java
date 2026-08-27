package com.scmcloud.order.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.order.domain.entity.OrdRefund;
import com.scmcloud.order.domain.entity.RefundStatus;
import com.scmcloud.order.mapper.OrdRefundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 退款单查询服务（{@code @Slave} 路由到只读副本）。
 *
 * @author SCM Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdRefundQueryService {

    private final OrdRefundMapper ordRefundMapper;

    @Slave
    public OrdRefund getById(UUID id) {
        return ordRefundMapper.selectById(id);
    }

    @Slave
    public List<OrdRefund> listByOrderId(UUID orderId) {
        if (orderId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<OrdRefund> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdRefund::getOrderId, orderId)
                .orderByDesc(OrdRefund::getCreateTime);
        return ordRefundMapper.selectList(wrapper);
    }

    /**
     * 自定义条件分页查询。
     */
    @Slave
    public Page<OrdRefund> pageWithWrapper(Page<OrdRefund> page,
                                            String refundNo,
                                            UUID orderId,
                                            String userId,
                                            RefundStatus status,
                                            LocalDateTime startTime,
                                            LocalDateTime endTime) {
        LambdaQueryWrapper<OrdRefund> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(refundNo != null && !refundNo.isBlank(), OrdRefund::getRefundNo, refundNo)
                .eq(orderId != null, OrdRefund::getOrderId, orderId)
                .eq(userId != null && !userId.isBlank(), OrdRefund::getUserId, userId)
                .eq(status != null, OrdRefund::getStatus, status.getCode())
                .ge(startTime != null, OrdRefund::getCreateTime, startTime)
                .le(endTime != null, OrdRefund::getCreateTime, endTime)
                .orderByDesc(OrdRefund::getCreateTime);
        return ordRefundMapper.selectPage(page, wrapper);
    }
}
