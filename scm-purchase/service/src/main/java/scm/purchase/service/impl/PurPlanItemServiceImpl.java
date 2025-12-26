package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurPlanItem;
import scm-purchase/service/src/main/java.mapper.PurPlanItemMapper;
import scm-purchase/service/src/main/java.service.IPurPlanItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurPlanItemService;

/**
 * <p>
 * 采购计划明细表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurPlanItemServiceImpl extends ServiceImpl<PurPlanItemMapper, PurPlanItem> implements IPurPlanItemService {

}
