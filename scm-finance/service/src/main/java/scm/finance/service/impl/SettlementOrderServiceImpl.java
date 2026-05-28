package scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.util.UUIDv7Util;
import scm.finance.domain.entity.SettlementOrder;
import scm.finance.mapper.SettlementOrderMapper;
import scm.finance.service.ISettlementOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class SettlementOrderServiceImpl extends ServiceImpl<SettlementOrderMapper, SettlementOrder>
        implements ISettlementOrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder createSettlement(SettlementOrder order) {
        log.info("创建结算单: partnerName={}, settlementType={}", order.getPartnerName(), order.getSettlementType());

        order.setId(UUIDv7Util.generateString());
        order.setSettlementNo(generateSettlementNo());
        order.setStatus(0);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setUnpaidAmount(order.getActualAmount());
        order.setDeleted(false);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        boolean success = save(order);
        if (!success) {
            throw new RuntimeException("创建结算单失败");
        }

        log.info("结算单创建成功: id={}, settlementNo={}", order.getId(), order.getSettlementNo());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder confirmSettlement(String id, String approverId, String approverName) {
        log.info("确认结算单: id={}, approver={}", id, approverName);

        SettlementOrder order = getById(id);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw new IllegalArgumentException("结算单不存在: " + id);
        }
        if (order.getStatus() != 0) {
            throw new IllegalStateException("只有待确认状态的结算单才能确认, 当前状态: " + order.getStatus());
        }

        order.setStatus(1);
        order.setApproverId(approverId);
        order.setApproverName(approverName);
        order.setApprovedAt(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        updateById(order);
        log.info("结算单确认成功: id={}", id);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder recordPayment(String id, BigDecimal amount) {
        log.info("记录结算单付款: id={}, amount={}", id, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("付款金额必须大于0");
        }

        SettlementOrder order = getById(id);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw new IllegalArgumentException("结算单不存在: " + id);
        }
        if (order.getStatus() != 1 && order.getStatus() != 2 && order.getStatus() != 3) {
            throw new IllegalStateException("当前状态不允许付款: " + order.getStatus());
        }

        BigDecimal newPaidAmount = order.getPaidAmount().add(amount);
        if (newPaidAmount.compareTo(order.getActualAmount()) > 0) {
            throw new IllegalArgumentException(
                    String.format("付款金额超出应付金额: 已付=%s, 本次=%s, 应付=%s",
                            order.getPaidAmount(), amount, order.getActualAmount()));
        }

        order.setPaidAmount(newPaidAmount);
        order.setUnpaidAmount(order.getActualAmount().subtract(newPaidAmount));
        order.setUpdateTime(LocalDateTime.now());

        if (order.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(4);
            log.info("结算单已全额付款: id={}", id);
        } else {
            order.setStatus(3);
            log.info("结算单部分付款: id={}, paid={}, unpaid={}", id, newPaidAmount, order.getUnpaidAmount());
        }

        updateById(order);
        return order;
    }

    @Override
    public Page<SettlementOrder> listByStatus(Integer status, int page, int size) {
        log.debug("按状态查询结算单: status={}, page={}, size={}", status, page, size);
        LambdaQueryWrapper<SettlementOrder> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(SettlementOrder::getStatus, status);
        }
        wrapper.eq(SettlementOrder::getDeleted, false);
        wrapper.orderByDesc(SettlementOrder::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    private String generateSettlementNo() {
        return "STL" + System.currentTimeMillis();
    }
}
