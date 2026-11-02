package com.scmcloud.inventory.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.inventory.domain.entity.InvAlert;
import com.scmcloud.inventory.dto.InvAlertQueryRequest;
import com.scmcloud.inventory.dto.InvAlertStatsResponse;
import com.scmcloud.inventory.mapper.InvAlertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvAlertQueryService {

    private final InvAlertMapper alertMapper;

    @Slave
    public InvAlert getById(String id) {
        return alertMapper.selectById(id);
    }

    @Slave
    public List<InvAlert> list() {
        return alertMapper.selectList(Wrappers.emptyWrapper());
    }

    @Slave
    public Page<InvAlert> page(int page, int size) {
        return alertMapper.selectPage(new Page<>(page, size), Wrappers.emptyWrapper());
    }

    @Slave
    public Page<InvAlert> query(InvAlertQueryRequest request) {
        LambdaQueryWrapper<InvAlert> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StringUtils.hasText(request.getSkuId()), InvAlert::getSkuId, request.getSkuId())
                .eq(StringUtils.hasText(request.getWarehouseId()), InvAlert::getWarehouseId, request.getWarehouseId())
                .eq(request.getAlertType() != null, InvAlert::getAlertType, request.getAlertType())
                .eq(request.getIsResolved() != null, InvAlert::getIsResolved, request.getIsResolved())
                .orderByDesc(InvAlert::getCreateTime);
        return alertMapper.selectPage(new Page<>(request.getPage(), request.getSize()), wrapper);
    }

    @Slave
    public InvAlertStatsResponse stats() {
        long total = alertMapper.selectCount(Wrappers.emptyWrapper());
        long unresolved = alertMapper.selectCount(
                Wrappers.<InvAlert>lambdaQuery().eq(InvAlert::getIsResolved, false));
        long lowStock = alertMapper.selectCount(
                Wrappers.<InvAlert>lambdaQuery()
                        .eq(InvAlert::getIsResolved, false)
                        .eq(InvAlert::getAlertType, 1));
        long outOfStock = alertMapper.selectCount(
                Wrappers.<InvAlert>lambdaQuery()
                        .eq(InvAlert::getIsResolved, false)
                        .eq(InvAlert::getAlertType, 2));
        return InvAlertStatsResponse.builder()
                .total(total)
                .unresolved(unresolved)
                .resolved(total - unresolved)
                .lowStock(lowStock)
                .outOfStock(outOfStock)
                .build();
    }

    @Slave
    public long count() {
        return alertMapper.selectCount(Wrappers.emptyWrapper());
    }
}
