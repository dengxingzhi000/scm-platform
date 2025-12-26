package scm.finance.service.impl;

import scm.finance.domain.entity.SettlementOrder;
import scm.finance.mapper.SettlementOrderMapper;
import scm.finance.service.ISettlementOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 结算单表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class SettlementOrderServiceImpl extends ServiceImpl<SettlementOrderMapper, SettlementOrder>
        implements ISettlementOrderService {

}
