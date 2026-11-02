package com.scmcloud.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.product.domain.entity.ProdBrand;

import java.util.List;

public interface IProdBrandService extends IService<ProdBrand> {

    List<ProdBrand> searchByName(String name);

    List<ProdBrand> listFeatured();

    Page<ProdBrand> pageByName(String name, Integer page, Integer size);

    ProdBrand createBrand(ProdBrand brand);

    void updateBrand(String id, ProdBrand brand);

    void deleteBrand(String id);
}
