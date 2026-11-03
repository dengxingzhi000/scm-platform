package com.cognora.platform.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.platform.product.domain.entity.ProdCategory;

import java.util.List;

public interface IProdCategoryService extends IService<ProdCategory> {

    List<ProdCategory> listByParentId(String parentId);

    List<ProdCategory> getCategoryTree();

    List<ProdCategory> searchByName(String name);

    Page<ProdCategory> pageByName(String name, Integer page, Integer size);

    ProdCategory createCategory(ProdCategory category);

    void updateCategory(String id, ProdCategory category);

    void deleteCategory(String id);
}