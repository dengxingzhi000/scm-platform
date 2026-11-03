package com.cognora.scm.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.scm.purchase.domain.entity.PurQuotationItem;

import java.util.List;

public interface IPurQuotationItemService extends IService<PurQuotationItem> {

    List<PurQuotationItem> listByQuotationId(String quotationId);

    boolean deleteByQuotationId(String quotationId);
}
