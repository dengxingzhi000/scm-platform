package scm.warehouse.service.impl;

import scm.warehouse.domain.entity.WmsLocation;
import scm.warehouse.mapper.WmsLocationMapper;
import scm.warehouse.service.IWmsLocationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 库位表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class WmsLocationServiceImpl extends ServiceImpl<WmsLocationMapper, WmsLocation> implements IWmsLocationService {

}
