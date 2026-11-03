package com.scmcloud.supplier.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.supplier.domain.entity.SupSupplierBankAccount;

import java.util.List;

public interface ISupSupplierBankAccountService extends IService<SupSupplierBankAccount> {

    List<SupSupplierBankAccount> listBySupplierId(String supplierId);

    SupSupplierBankAccount create(String supplierId, SupSupplierBankAccount account);

    SupSupplierBankAccount update(String id, SupSupplierBankAccount account);

    boolean delete(String id);

    boolean setDefault(String id);
}
