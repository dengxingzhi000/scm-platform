package com.scmcloud.finance.service;

import com.scmcloud.finance.domain.entity.SettlementItem;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

public interface ISettlementItemService extends IService<SettlementItem> {

    List<SettlementItem> listBySettlementId(String settlementId);
}
