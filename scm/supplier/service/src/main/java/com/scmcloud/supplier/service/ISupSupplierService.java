package com.scmcloud.supplier.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.domain.entity.SupSupplierCategory;
import com.scmcloud.supplier.domain.entity.SupSupplierChangeLog;
import com.scmcloud.supplier.dto.SupplierAuditRequest;
import com.scmcloud.supplier.dto.SupplierPerformanceVO;
import com.scmcloud.supplier.dto.SupplierProfileVO;
import com.scmcloud.supplier.dto.SupplierStatsVO;
import com.scmcloud.supplier.dto.SupplierUpsertRequest;

import java.util.List;

public interface ISupSupplierService extends IService<SupSupplier> {

    Page<SupSupplier> pageList(int page, int size, String keyword, Integer supplierType,
                               Integer cooperationStatus, Boolean enabled, Integer auditStatus,
                               Integer supplierLevel, Integer riskLevel, Boolean blacklisted,
                               String categoryId);

    List<SupSupplier> listActive();

    List<SupSupplier> searchByName(String name);

    SupSupplier createSupplier(SupplierUpsertRequest request);

    SupSupplier updateSupplier(String id, SupplierUpsertRequest request);

    void deleteSupplier(String id, String operatorId, String operatorName);

    boolean enableSupplier(String id);

    boolean disableSupplier(String id);

    void submitAudit(String id, String operatorId, String operatorName);

    void audit(String id, SupplierAuditRequest request);

    void blacklist(String id, String reason, String operatorId, String operatorName);

    void unblacklist(String id, String operatorId, String operatorName);

    void changeRiskLevel(String id, Integer riskLevel, String reason,
                         String operatorId, String operatorName);

    void changeCooperationStatus(String id, Integer status, String operatorId, String operatorName);

    SupplierProfileVO getProfile(String id);

    SupplierStatsVO getStats();

    SupplierPerformanceVO getPerformance(String id);

    List<SupSupplierChangeLog> listChangeLogs(String id, int limit);

    List<SupSupplierCategory> getCategories(String id);

    void assignCategories(String id, List<String> categoryIds, String operatorId, String operatorName);
}
