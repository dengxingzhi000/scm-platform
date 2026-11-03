package com.scmcloud.finance.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.finance.domain.entity.SettlementOrder;
import com.scmcloud.finance.mapper.SettlementOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementOrderQueryService {
    private final SettlementOrderMapper settlementOrderMapper;

    @Slave
    public Page<SettlementOrder> listByStatus(Integer status, int page, int size) {
        log.debug("按状态查询结算单: status={}, page={}, size={}", status, page, size);
        LambdaQueryWrapper<SettlementOrder> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(SettlementOrder::getStatus, status);
        }
        wrapper.eq(SettlementOrder::getDeleted, false);
        wrapper.orderByDesc(SettlementOrder::getCreateTime);
        return settlementOrderMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
