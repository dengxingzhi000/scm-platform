package com.scmcloud.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.purchase.domain.entity.PurOrderItem;

import java.util.List;

public interface IPurOrderItemService extends IService<PurOrderItem> {

    List<PurOrderItem> listByOrderId(String orderId);

    boolean deleteByOrderId(String orderId);
}
