package scm.warehouse.service.impl;

import scm.warehouse.domain.entity.WmsOutboundItem;
import scm.warehouse.mapper.WmsOutboundItemMapper;
import scm.warehouse.service.IWmsOutboundItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 出库单明细表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class WmsOutboundItemServiceImpl extends ServiceImpl<WmsOutboundItemMapper, WmsOutboundItem>
        implements IWmsOutboundItemService {

}
