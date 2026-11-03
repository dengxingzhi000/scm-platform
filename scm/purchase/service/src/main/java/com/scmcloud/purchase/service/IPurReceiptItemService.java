package com.scmcloud.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.purchase.domain.entity.PurReceiptItem;

import java.util.List;

public interface IPurReceiptItemService extends IService<PurReceiptItem> {

    List<PurReceiptItem> listByReceiptId(String receiptId);

    boolean deleteByReceiptId(String receiptId);
}
