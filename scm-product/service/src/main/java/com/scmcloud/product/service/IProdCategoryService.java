package com.scmcloud.product.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.product.domain.entity.ProdCategory;

import java.util.List;

public interface IProdCategoryService extends IService<ProdCategory> {

    List<ProdCategory> listByParentId(String parentId);

    List<ProdCategory> getCategoryTree();

    List<ProdCategory> searchByName(String name);
}
