package com.scmcloud.supplier.service;

import com.scmcloud.supplier.domain.entity.SupPurchaseOrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

public interface ISupPurchaseOrderItemService extends IService<SupPurchaseOrderItem> {

    Page<SupPurchaseOrderItem> pageList(int page, int size, String purchaseId, String skuId);

    List<SupPurchaseOrderItem> listByPurchaseId(String purchaseId);
}
