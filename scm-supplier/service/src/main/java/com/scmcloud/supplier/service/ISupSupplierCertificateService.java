package com.scmcloud.supplier.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.supplier.domain.entity.SupSupplierCertificate;

import java.util.List;

public interface ISupSupplierCertificateService extends IService<SupSupplierCertificate> {

    List<SupSupplierCertificate> listBySupplierId(String supplierId);

    SupSupplierCertificate create(String supplierId, SupSupplierCertificate certificate);

    SupSupplierCertificate update(String id, SupSupplierCertificate certificate);

    boolean delete(String id);

    void verify(String id, boolean approved, String operatorId, String operatorName, String remark);

    Page<SupSupplierCertificate> pageExpiring(int page, int size, int days);

    long countExpiring(int days);
}
