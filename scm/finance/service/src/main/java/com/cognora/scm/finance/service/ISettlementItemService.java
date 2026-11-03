package com.cognora.scm.finance.service;

import com.cognora.scm.finance.domain.entity.SettlementItem;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

public interface ISettlementItemService extends IService<SettlementItem> {

    List<SettlementItem> listBySettlementId(String settlementId);

    SettlementItem createItem(SettlementItem item);

    SettlementItem updateItem(String id, SettlementItem item);
}
