package com.scmcloud.supplier.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.supplier.domain.entity.SupSupplierEvaluation;

import java.math.BigDecimal;
import java.util.List;

public interface ISupSupplierEvaluationService extends IService<SupSupplierEvaluation> {

    Page<SupSupplierEvaluation> pageList(int page, int size, String supplierId, String evaluationPeriod);

    List<SupSupplierEvaluation> listBySupplierId(String supplierId);

    BigDecimal calculateAverageScore(String supplierId);

    SupSupplierEvaluation createEvaluation(SupSupplierEvaluation evaluation);

    SupSupplierEvaluation updateEvaluation(String id, SupSupplierEvaluation evaluation);

    boolean deleteEvaluation(String id);

    /** 根据评价记录回写供应商的质量/交付/服务/综合得分与分级。 */
    void refreshSupplierScorecard(String supplierId);
}
