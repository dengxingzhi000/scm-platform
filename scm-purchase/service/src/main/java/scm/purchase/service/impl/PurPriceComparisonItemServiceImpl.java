package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurPriceComparisonItem;
import scm-purchase/service/src/main/java.mapper.PurPriceComparisonItemMapper;
import scm-purchase/service/src/main/java.service.IPurPriceComparisonItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurPriceComparisonItemService;

/**
 * <p>
 * 比价明细表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurPriceComparisonItemServiceImpl extends ServiceImpl<PurPriceComparisonItemMapper, PurPriceComparisonItem> implements IPurPriceComparisonItemService {

}
