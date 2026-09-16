package com.scmcloud.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.finance.domain.entity.SettlementItem;
import com.scmcloud.finance.mapper.SettlementItemMapper;
import com.scmcloud.finance.service.ISettlementItemService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SettlementItemServiceImpl extends ServiceImpl<SettlementItemMapper, SettlementItem>
        implements ISettlementItemService {

    @Override
    public List<SettlementItem> listBySettlementId(String settlementId) {
        log.debug("查询结算明细: settlementId={}", settlementId);
        LambdaQueryWrapper<SettlementItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SettlementItem::getSettlementId, settlementId)
                .orderByDesc(SettlementItem::getDocumentDate);
        return list(wrapper);
    }
}
