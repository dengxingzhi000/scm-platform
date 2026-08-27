package com.scmcloud.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrdOrderItem;

import java.util.List;
import java.util.UUID;

public interface IOrdOrderService extends IService<OrdOrder> {

    OrdOrder createOrder(OrdOrder order, List<OrdOrderItem> items);

    boolean updateOrderStatus(UUID orderId, Integer status);

    List<OrdOrder> listByUserId(String userId);

    Page<OrdOrder> pageByUserId(String userId, Integer pageNum, Integer pageSize);
}
