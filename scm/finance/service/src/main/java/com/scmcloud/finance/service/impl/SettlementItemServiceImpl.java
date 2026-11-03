package com.scmcloud.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.finance.domain.entity.SettlementItem;
import com.scmcloud.finance.mapper.SettlementItemMapper;
import com.scmcloud.finance.service.ISettlementItemService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SettlementItemServiceImpl extends ServiceImpl<SettlementItemMapper, SettlementItem>
        implements ISettlementItemService {

    @Override
    public List<SettlementItem> listBySettlementId(String settlementId) {
        log.debug("查询结算明细: settlementId={}", (settlementId + "").replace("\r", "_").replace("\n", "_"));
        LambdaQueryWrapper<SettlementItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SettlementItem::getSettlementId, settlementId)
                .orderByDesc(SettlementItem::getDocumentDate);
        return list(wrapper);
    }

    @Override
    public SettlementItem createItem(SettlementItem item) {
        item.setId(UUIDv7Util.generateString());
        item.setCreateTime(LocalDateTime.now());
        save(item);
        log.info("结算明细创建成功: id={}, settlementId={}", (item.getId() + "").replace("\r", "_").replace("\n", "_"), (item.getSettlementId() + "").replace("\r", "_").replace("\n", "_"));
        return item;
    }

    @Override
    public SettlementItem updateItem(String id, SettlementItem item) {
        item.setId(id);
        updateById(item);
        log.info("结算明细更新成功: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return item;
    }
}
