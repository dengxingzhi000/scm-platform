package com.cognora.scm.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.scm.purchase.domain.entity.PurReceiptItem;

import java.util.List;

public interface IPurReceiptItemService extends IService<PurReceiptItem> {

    List<PurReceiptItem> listByReceiptId(String receiptId);

    boolean deleteByReceiptId(String receiptId);
}
