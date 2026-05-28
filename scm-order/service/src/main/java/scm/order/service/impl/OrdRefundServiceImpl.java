package scm.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scm.order.domain.entity.OrdRefund;
import scm.order.mapper.OrdRefundMapper;
import scm.order.service.IOrdRefundService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrdRefundServiceImpl extends ServiceImpl<OrdRefundMapper, OrdRefund> implements IOrdRefundService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrdRefund createRefund(OrdRefund refund) {
        log.info("创建退款记录: orderNo={}, refundAmount={}", refund.getOrderNo(), refund.getRefundAmount());

        refund.setId(UUID.randomUUID().toString());
        refund.setStatus(0);
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());

        boolean saved = save(refund);
        if (!saved) {
            throw new RuntimeException("创建退款记录失败");
        }

        log.info("退款记录创建成功: id={}, refundNo={}", refund.getId(), refund.getRefundNo());
        return refund;
    }

    @Override
    public List<OrdRefund> listByOrderId(Long orderId) {
        log.debug("查询订单退款记录: orderId={}", orderId);
        LambdaQueryWrapper<OrdRefund> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdRefund::getOrderId, orderId)
                .orderByDesc(OrdRefund::getCreateTime);
        return list(wrapper);
    }
}
