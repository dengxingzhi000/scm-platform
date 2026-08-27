package com.scmcloud.order.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.order.domain.entity.OrdRefund;
import com.scmcloud.order.domain.entity.OrdRefundItem;
import com.scmcloud.order.domain.entity.RefundStatus;
import com.scmcloud.order.mapper.OrdRefundMapper;
import com.scmcloud.order.service.IOrdRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 退款命令服务（{@code @Master} + 事务）。
 *
 * <p>仅暴露业务方法：</p>
 * <ul>
 *   <li>{@link #createRefund(OrdRefund)} 创建退款单 + 子表</li>
 *   <li>{@link #approveRefund(UUID, String, String, String)} 审核通过</li>
 *   <li>{@link #rejectRefund(UUID, String, String, String)} 拒绝退款</li>
 *   <li>{@link #completeRefund(UUID)} 完成退款(线下已退款成功后回调)</li>
 * </ul>
 *
 * <p><b>不</b>暴露任意字段更新 / 删除入口。</p>
 *
 * @author SCM Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdRefundCommandService {

    private final OrdRefundMapper ordRefundMapper;
    private final OrdRefundItemCommandService ordRefundItemCommandService;

    /**
     * 通过旧 IService 接口的 createRefund 方法（兼容 OrderDubboServiceImpl 调用）。
     */
    @Master(reason = "创建退款")
    @Transactional(rollbackFor = Exception.class)
    public OrdRefund createRefund(OrdRefund refund) {
        return doCreateRefund(refund);
    }

    private OrdRefund doCreateRefund(OrdRefund refund) {
        log.info("创建退款: refundNo={}, orderId={}", refund.getRefundNo(), refund.getOrderId());

        refund.setStatusEnum(RefundStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        refund.setCreateTime(now);
        refund.setUpdateTime(now);

        int saved = ordRefundMapper.insert(refund);
        if (saved <= 0) {
            throw new RuntimeException("创建退款记录失败");
        }

        if (refund.getItems() != null && !refund.getItems().isEmpty()) {
            for (OrdRefundItem item : refund.getItems()) {
                item.setRefundId(refund.getId());
                item.setRefundNo(refund.getRefundNo());
                item.setOrderId(refund.getOrderId());
                item.setOrderNo(refund.getOrderNo());
            }
            ordRefundItemCommandService.saveBatch(refund.getItems());
        }
        return refund;
    }

    @Master(reason = "审核通过退款")
    @Transactional(rollbackFor = Exception.class)
    public boolean approveRefund(UUID refundId, String handlerId, String handlerName, String handlerRemark) {
        log.info("审核通过退款: refundId={}, handler={}", refundId, handlerId);
        OrdRefund refund = selectOrThrow(refundId);

        if (!refund.getStatusEnum().isReviewable()) {
            throw new IllegalStateException("退款单当前状态不可审核: " + refund.getStatusEnum());
        }
        refund.setStatusEnum(RefundStatus.APPROVED);
        refund.setHandlerId(handlerId);
        refund.setHandlerName(handlerName);
        refund.setHandlerRemark(handlerRemark);
        refund.setApprovedAt(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        return ordRefundMapper.updateById(refund) > 0;
    }

    @Master(reason = "拒绝退款")
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectRefund(UUID refundId, String handlerId, String handlerName, String handlerRemark) {
        log.info("拒绝退款: refundId={}, handler={}", refundId, handlerId);
        OrdRefund refund = selectOrThrow(refundId);

        if (!refund.getStatusEnum().isReviewable()) {
            throw new IllegalStateException("退款单当前状态不可审核: " + refund.getStatusEnum());
        }
        refund.setStatusEnum(RefundStatus.REJECTED);
        refund.setHandlerId(handlerId);
        refund.setHandlerName(handlerName);
        refund.setHandlerRemark(handlerRemark);
        refund.setRejectedAt(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        return ordRefundMapper.updateById(refund) > 0;
    }

    @Master(reason = "完成退款")
    @Transactional(rollbackFor = Exception.class)
    public boolean completeRefund(UUID refundId) {
        log.info("完成退款: refundId={}", refundId);
        OrdRefund refund = selectOrThrow(refundId);

        if (refund.getStatusEnum() != RefundStatus.APPROVED) {
            throw new IllegalStateException("仅 APPROVED 状态可完成: " + refund.getStatusEnum());
        }
        refund.setStatusEnum(RefundStatus.COMPLETED);
        refund.setCompletedAt(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        return ordRefundMapper.updateById(refund) > 0;
    }

    private OrdRefund selectOrThrow(UUID refundId) {
        OrdRefund refund = ordRefundMapper.selectById(refundId);
        if (refund == null) {
            throw new IllegalArgumentException("退款单不存在: id=" + refundId);
        }
        return refund;
    }
}
