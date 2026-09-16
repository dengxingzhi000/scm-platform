package com.scmcloud.supplier.service;

import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

public interface ISupSupplierService extends IService<SupSupplier> {

    Page<SupSupplier> pageList(int page, int size, String keyword, Integer supplierType,
                               Integer cooperationStatus, Boolean enabled);

    List<SupSupplier> listActive();

    List<SupSupplier> searchByName(String name);

    boolean enableSupplier(String id);

    boolean disableSupplier(String id);
}
