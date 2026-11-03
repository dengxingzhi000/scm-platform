package com.scmcloud.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.purchase.domain.entity.PurPriceComparisonItem;

import java.util.List;

public interface IPurPriceComparisonItemService extends IService<PurPriceComparisonItem> {

    List<PurPriceComparisonItem> listByComparisonId(String comparisonId);

    boolean deleteByComparisonId(String comparisonId);
}
