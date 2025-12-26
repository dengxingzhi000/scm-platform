package scm.warehouse.service.impl;

import scm.warehouse.domain.entity.WmsWarehouse;
import scm.warehouse.mapper.WmsWarehouseMapper;
import scm.warehouse.service.IWmsWarehouseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 仓库表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class WmsWarehouseServiceImpl extends ServiceImpl<WmsWarehouseMapper, WmsWarehouse>
        implements IWmsWarehouseService {

}
