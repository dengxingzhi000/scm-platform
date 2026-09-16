package com.scmcloud.product.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.product.domain.entity.ProdBrand;

import java.util.List;

public interface IProdBrandService extends IService<ProdBrand> {

    List<ProdBrand> searchByName(String name);

    List<ProdBrand> listFeatured();
}
