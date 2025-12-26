package scm.tenant.service.impl;

import scm.tenant.domain.entity.TenantFeature;
import scm.tenant.mapper.TenantFeatureMapper;
import scm.tenant.service.ITenantFeatureService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户功能开关表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class TenantFeatureServiceImpl extends ServiceImpl<TenantFeatureMapper, TenantFeature> implements ITenantFeatureService {

}
