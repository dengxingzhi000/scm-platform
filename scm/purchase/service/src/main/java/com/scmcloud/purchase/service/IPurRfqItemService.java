package com.scmcloud.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.purchase.domain.entity.PurRfqItem;

import java.util.List;

public interface IPurRfqItemService extends IService<PurRfqItem> {

    List<PurRfqItem> listByRfqId(String rfqId);

    boolean deleteByRfqId(String rfqId);
}
