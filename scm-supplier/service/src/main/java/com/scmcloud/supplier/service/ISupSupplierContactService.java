package com.scmcloud.supplier.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.supplier.domain.entity.SupSupplierContact;

import java.util.List;

public interface ISupSupplierContactService extends IService<SupSupplierContact> {

    List<SupSupplierContact> listBySupplierId(String supplierId);

    SupSupplierContact create(String supplierId, SupSupplierContact contact);

    SupSupplierContact update(String id, SupSupplierContact contact);

    boolean delete(String id);

    boolean setPrimary(String id);
}
