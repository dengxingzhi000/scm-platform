package com.scmcloud.finance.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.finance.domain.entity.SettlementItem;
import com.scmcloud.finance.mapper.SettlementItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementItemQueryService {
    private final SettlementItemMapper settlementItemMapper;

    @Slave
    public List<SettlementItem> listBySettlementId(String settlementId) {
        log.debug("查询结算明细: settlementId={}", settlementId);
        LambdaQueryWrapper<SettlementItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SettlementItem::getSettlementId, settlementId)
                .orderByDesc(SettlementItem::getDocumentDate);
        return settlementItemMapper.selectList(wrapper);
    }
}
