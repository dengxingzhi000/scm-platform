package com.scmcloud.mall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.mall.domain.entity.Seller;

public interface ISellerService extends IService<Seller> {

    Seller registerSeller(Seller seller);

    Seller getSeller(Long sellerId);
}
