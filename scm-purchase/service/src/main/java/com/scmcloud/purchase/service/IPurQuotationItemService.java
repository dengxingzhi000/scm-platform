package com.scmcloud.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.purchase.domain.entity.PurQuotationItem;

import java.util.List;

public interface IPurQuotationItemService extends IService<PurQuotationItem> {

    List<PurQuotationItem> listByQuotationId(String quotationId);

    boolean deleteByQuotationId(String quotationId);
}
