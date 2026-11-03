package com.cognora.ecom.mall.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.ecom.mall.domain.entity.Seller;

public interface ISellerService extends IService<Seller> {

    Seller registerSeller(Seller seller);

    Seller getSeller(Long sellerId);
}
