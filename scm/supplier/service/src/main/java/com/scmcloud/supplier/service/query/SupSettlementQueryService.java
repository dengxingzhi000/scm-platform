package com.scmcloud.supplier.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.supplier.domain.entity.SupSettlement;
import com.scmcloud.supplier.mapper.SupSettlementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSettlementQueryService {

    private final SupSettlementMapper supSettlementMapper;

    @Slave
    public SupSettlement getById(String id) {
        return supSettlementMapper.selectById(id);
    }

    @Slave
    public List<SupSettlement> listAll() {
        return supSettlementMapper.selectList(null);
    }

    @Slave
    public Page<SupSettlement> pageQuery(Page<SupSettlement> page, Wrapper<SupSettlement> wrapper) {
        return supSettlementMapper.selectPage(page, wrapper);
    }

    @Slave
    public Page<SupSettlement> pageList(int page, int size, String supplierId, Integer status, String settlementPeriod) {
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
        return supSettlementMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<SupSettlement> listBySupplierId(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            return List.of();
        }
        log.debug("查询供应商的所有对账单: supplierId={}", supplierId);
        LambdaQueryWrapper<SupSettlement> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SupSettlement::getSupplierId, supplierId);
        wrapper.eq(SupSettlement::getDeleted, false);
        wrapper.orderByDesc(SupSettlement::getCreateTime);
        return supSettlementMapper.selectList(wrapper);
    }
}
