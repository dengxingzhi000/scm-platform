package scm.purchase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.purchase.domain.entity.PurPriceComparisonItem;

import java.util.List;

public interface IPurPriceComparisonItemService extends IService<PurPriceComparisonItem> {

    List<PurPriceComparisonItem> listByComparisonId(String comparisonId);

    boolean deleteByComparisonId(String comparisonId);
}
