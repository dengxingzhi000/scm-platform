package com.scmcloud.supplier.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.supplier.domain.entity.SupSupplierChangeLog;

import java.util.List;

public interface ISupSupplierChangeLogService extends IService<SupSupplierChangeLog> {

    void record(String supplierId, String changeType, String changeContent,
                String reason, String operatorId, String operatorName);

    List<SupSupplierChangeLog> listBySupplierId(String supplierId, int limit);
}
