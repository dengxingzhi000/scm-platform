package com.scmcloud.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.promotion.domain.entity.FlashSale;

import java.util.List;

public interface IFlashSaleService extends IService<FlashSale> {

    List<FlashSale> getActiveFlashSales();

    boolean checkStock(Long flashSaleId, Long skuId);

    boolean deductStock(Long flashSaleId, Long skuId, Integer quantity);

    FlashSale getByProductAndSku(Long productId, Long skuId);
}
