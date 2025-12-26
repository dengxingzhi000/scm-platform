package scm.warehouse.service.impl;

import scm.warehouse.domain.entity.WmsWavePicking;
import scm.warehouse.mapper.WmsWavePickingMapper;
import scm.warehouse.service.IWmsWavePickingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 波次拣货表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class WmsWavePickingServiceImpl extends ServiceImpl<WmsWavePickingMapper, WmsWavePicking>
        implements IWmsWavePickingService {

}
