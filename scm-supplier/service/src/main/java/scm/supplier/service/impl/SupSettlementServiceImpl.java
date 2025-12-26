package scm.supplier.service.impl;

import scm.supplier.domain.entity.SupSettlement;
import scm.supplier.mapper.SupSettlementMapper;
import scm.supplier.service.ISupSettlementService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 对账单表 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Service
public class SupSettlementServiceImpl extends ServiceImpl<SupSettlementMapper, SupSettlement>
        implements ISupSettlementService {

}
