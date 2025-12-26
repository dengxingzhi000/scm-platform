package scm.warehouse.service.impl;

import scm.warehouse.domain.entity.WmsInbound;
import scm.warehouse.mapper.WmsInboundMapper;
import scm.warehouse.service.IWmsInboundService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 入库单表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class WmsInboundServiceImpl extends ServiceImpl<WmsInboundMapper, WmsInbound> implements IWmsInboundService {

}
