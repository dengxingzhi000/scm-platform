package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurPlan;
import scm-purchase/service/src/main/java.mapper.PurPlanMapper;
import scm-purchase/service/src/main/java.service.IPurPlanService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurPlanService;

/**
 * <p>
 * 采购计划表（MRP） 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurPlanServiceImpl extends ServiceImpl<PurPlanMapper, PurPlan> implements IPurPlanService {

}
