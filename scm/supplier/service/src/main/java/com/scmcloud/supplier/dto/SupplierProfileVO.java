package com.scmcloud.supplier.dto;

import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.domain.entity.SupSupplierBankAccount;
import com.scmcloud.supplier.domain.entity.SupSupplierCategory;
import com.scmcloud.supplier.domain.entity.SupSupplierCertificate;
import com.scmcloud.supplier.domain.entity.SupSupplierChangeLog;
import com.scmcloud.supplier.domain.entity.SupSupplierContact;
import lombok.Data;

import java.util.List;

/**
 * 供应商 360° 画像：主数据 + 联系人 + 银行账户 + 资质 + 分类 + 最近变更。
 */
@Data
public class SupplierProfileVO {

    private SupSupplier supplier;
    private List<SupSupplierContact> contacts;
    private List<SupSupplierBankAccount> bankAccounts;
    private List<SupSupplierCertificate> certificates;
    private List<SupSupplierCategory> categories;
    private List<SupSupplierChangeLog> changeLogs;
}
