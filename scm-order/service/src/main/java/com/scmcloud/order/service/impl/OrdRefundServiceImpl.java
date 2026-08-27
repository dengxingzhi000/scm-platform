package com.scmcloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.order.domain.entity.OrdRefund;
import com.scmcloud.order.domain.entity.OrdRefundItem;
import com.scmcloud.order.domain.entity.RefundStatus;
import com.scmcloud.order.mapper.OrdRefundMapper;
import com.scmcloud.order.service.IOrdRefundService;
import com.scmcloud.order.service.command.OrdRefundItemCommandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrdRefundServiceImpl extends ServiceImpl<OrdRefundMapper, OrdRefund> implements IOrdRefundService {

    @Autowired
    private StatusValidator statusValidator;

    @Autowired
    private OrdRefundItemCommandService ordRefundItemCommandService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrdRefund createRefund(OrdRefund refund) {
        log.info("创建退款记录 orderNo={}, refundAmount={}", refund.getOrderNo(), refund.getRefundAmount());

        refund.setStatusEnum(RefundStatus.PENDING);
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());

        boolean saved = save(refund);
        if (!saved) {
            throw new RuntimeException("创建退款记录失败");
        }

        // 在同一事务内写入退款明细子表
        if (refund.getItems() != null && !refund.getItems().isEmpty()) {
            for (OrdRefundItem item : refund.getItems()) {
                item.setRefundId(refund.getId());
                item.setRefundNo(refund.getRefundNo());
                item.setOrderId(refund.getOrderId());
                item.setOrderNo(refund.getOrderNo());
            }
            ordRefundItemCommandService.saveBatch(refund.getItems());
        }

        log.info("退款记录创建成功 id={}, refundNo={}", refund.getId(), refund.getRefundNo());
        return refund;
    }

    @Override
    public List<OrdRefund> listByOrderId(UUID orderId) {
        log.debug("查询订单退款记录 orderId={}", orderId);
        LambdaQueryWrapper<OrdRefund> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdRefund::getOrderId, orderId)
                .orderByDesc(OrdRefund::getCreateTime);
        return list(wrapper);
    }
}
