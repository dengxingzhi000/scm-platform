package scm.logistics.service.impl;

import scm.logistics.domain.entity.TmsTracking;
import scm.logistics.mapper.TmsTrackingMapper;
import scm.logistics.service.ITmsTrackingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 物流轨迹表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class TmsTrackingServiceImpl extends ServiceImpl<TmsTrackingMapper, TmsTracking> implements ITmsTrackingService {

}
