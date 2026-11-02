package com.scmcloud.supplier.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.supplier.domain.entity.SupSupplierCategory;

import java.util.List;

public interface ISupSupplierCategoryService extends IService<SupSupplierCategory> {

    List<SupSupplierCategory> listCategories(String keyword, Boolean enabled);

    SupSupplierCategory createCategory(SupSupplierCategory category);

    SupSupplierCategory updateCategory(String id, SupSupplierCategory category);

    void deleteCategory(String id);
}
