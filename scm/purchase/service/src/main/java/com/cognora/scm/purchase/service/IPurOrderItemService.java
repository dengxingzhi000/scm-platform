package com.cognora.scm.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.scm.purchase.domain.entity.PurOrderItem;

import java.util.List;

public interface IPurOrderItemService extends IService<PurOrderItem> {

    List<PurOrderItem> listByOrderId(String orderId);

    boolean deleteByOrderId(String orderId);
}
