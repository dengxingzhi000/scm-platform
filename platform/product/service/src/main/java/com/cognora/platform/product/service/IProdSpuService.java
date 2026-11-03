package com.cognora.platform.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.platform.product.domain.entity.ProdSpu;

import java.util.List;

public interface IProdSpuService extends IService<ProdSpu> {

    List<ProdSpu> listByCategoryId(String categoryId);

    List<ProdSpu> listByBrandId(String brandId);

    Page<ProdSpu> search(String keyword, String categoryId, String brandId, Integer status,
                         Integer page, Integer size);

    ProdSpu createSpu(ProdSpu spu);

    void updateSpu(String id, ProdSpu spu);

    void deleteSpu(String id);
}