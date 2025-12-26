package scm.warehouse.service.impl;

import scm.warehouse.domain.entity.WmsOutbound;
import scm.warehouse.mapper.WmsOutboundMapper;
import scm.warehouse.service.IWmsOutboundService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 出库单表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class WmsOutboundServiceImpl extends ServiceImpl<WmsOutboundMapper, WmsOutbound> implements IWmsOutboundService {

}
