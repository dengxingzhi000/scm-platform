package scm.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import scm.supplier.domain.entity.SupSettlement;
import scm.supplier.mapper.SupSettlementMapper;
import scm.supplier.service.ISupSettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SupSettlementServiceImpl extends ServiceImpl<SupSettlementMapper, SupSettlement>
        implements ISupSettlementService {

    @Override
    public Page<SupSettlement> pageList(int page, int size, String supplierId, Integer status,
                                        String settlementPeriod) {
        log.debug("分页查询对账单: page={}, size={}, supplierId={}, status={}", page, size, supplierId, status);

        LambdaQueryWrapper<SupSettlement> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(SupSettlement::getSupplierId, supplierId);
        }
        if (status != null) {
            wrapper.eq(SupSettlement::getStatus, status);
        }
        if (StringUtils.hasText(settlementPeriod)) {
            wrapper.eq(SupSettlement::getSettlementPeriod, settlementPeriod);
        }

        wrapper.eq(SupSettlement::getDeleted, false);
        wrapper.orderByDesc(SupSettlement::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<SupSettlement> listBySupplierId(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            return List.of();
        }
        log.debug("查询供应商的所有对账单: supplierId={}", supplierId);
        return lambdaQuery()
                .eq(SupSettlement::getSupplierId, supplierId)
                .eq(SupSettlement::getDeleted, false)
                .orderByDesc(SupSettlement::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(String id, String approverId, String approverName) {
        log.info("确认对账单: id={}, approverId={}", id, approverId);

        SupSettlement settlement = getById(id);
        if (settlement == null) {
            log.warn("对账单不存在: id={}", id);
            return false;
        }
        if (settlement.getStatus() != 0) {
            log.warn("对账单状态不允许确认: id={}, status={}", id, settlement.getStatus());
            return false;
        }

        settlement.setStatus(1);
        settlement.setApproverId(approverId);
        settlement.setApproverName(approverName);
        settlement.setApprovedAt(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());

        boolean success = updateById(settlement);
        if (success) {
            log.info("对账单确认成功: id={}", id);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsPaid(String id, String updateBy) {
        log.info("标记对账单已付款: id={}", id);

        SupSettlement settlement = getById(id);
        if (settlement == null) {
            log.warn("对账单不存在: id={}", id);
            return false;
        }
        if (settlement.getStatus() != 1 && settlement.getStatus() != 2 && settlement.getStatus() != 3) {
            log.warn("对账单状态不允许标记付款: id={}, status={}", id, settlement.getStatus());
            return false;
        }

        settlement.setStatus(4);
        settlement.setPaymentAmount(settlement.getActualAmount());
        settlement.setUpdateTime(LocalDateTime.now());
        settlement.setUpdateBy(updateBy);

        boolean success = updateById(settlement);
        if (success) {
            log.info("对账单标记付款成功: id={}", id);
        }
        return success;
    }
}
