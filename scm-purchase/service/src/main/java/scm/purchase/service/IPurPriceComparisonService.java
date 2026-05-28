package scm.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import scm.purchase.domain.entity.PurPriceComparison;

import java.util.List;

public interface IPurPriceComparisonService extends IService<PurPriceComparison> {

    PurPriceComparison getByComparisonNo(String comparisonNo);

    Page<PurPriceComparison> pageQuery(int page, int size, Integer status, String rfqId);

    List<PurPriceComparison> listByRfqId(String rfqId);

    boolean approve(String id, String approverId, String approverName);
}
