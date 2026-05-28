package scm.supplier.service;

import scm.supplier.domain.entity.SupSupplierEvaluation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface ISupSupplierEvaluationService extends IService<SupSupplierEvaluation> {

    Page<SupSupplierEvaluation> pageList(int page, int size, String supplierId, String evaluationPeriod);

    List<SupSupplierEvaluation> listBySupplierId(String supplierId);

    BigDecimal calculateAverageScore(String supplierId);
}
