package com.scmcloud.product.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.product.domain.entity.ProdSku;

import java.util.List;

public interface IProdSkuService extends IService<ProdSku> {

    List<ProdSku> listBySpuId(String spuId);

    List<ProdSku> listBySpuIds(List<String> spuIds);
}
