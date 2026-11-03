package com.cognora.scm.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.scm.purchase.domain.entity.PurPriceComparisonItem;

import java.util.List;

public interface IPurPriceComparisonItemService extends IService<PurPriceComparisonItem> {

    List<PurPriceComparisonItem> listByComparisonId(String comparisonId);

    boolean deleteByComparisonId(String comparisonId);
}
