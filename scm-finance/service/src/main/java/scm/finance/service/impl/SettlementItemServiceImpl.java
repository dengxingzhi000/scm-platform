package scm.finance.service.impl;

import scm.finance.domain.entity.SettlementItem;
import scm.finance.mapper.SettlementItemMapper;
import scm.finance.service.ISettlementItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 结算明细表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class SettlementItemServiceImpl extends ServiceImpl<SettlementItemMapper, SettlementItem>
        implements ISettlementItemService {

}
