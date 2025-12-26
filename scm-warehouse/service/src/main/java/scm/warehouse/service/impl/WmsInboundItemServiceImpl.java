package scm.warehouse.service.impl;

import scm.warehouse.domain.entity.WmsInboundItem;
import scm.warehouse.mapper.WmsInboundItemMapper;
import scm.warehouse.service.IWmsInboundItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 入库单明细表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class WmsInboundItemServiceImpl extends ServiceImpl<WmsInboundItemMapper, WmsInboundItem>
        implements IWmsInboundItemService {

}
