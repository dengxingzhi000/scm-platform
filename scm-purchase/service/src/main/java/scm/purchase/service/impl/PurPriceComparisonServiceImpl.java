package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurPriceComparison;
import scm-purchase/service/src/main/java.mapper.PurPriceComparisonMapper;
import scm-purchase/service/src/main/java.service.IPurPriceComparisonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurPriceComparisonService;

/**
 * <p>
 * 比价分析表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurPriceComparisonServiceImpl extends ServiceImpl<PurPriceComparisonMapper, PurPriceComparison> implements IPurPriceComparisonService {

}
