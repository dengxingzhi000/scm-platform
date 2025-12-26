package scm.tenant.service.impl;

import scm.tenant.domain.entity.TenantConfig;
import scm.tenant.mapper.TenantConfigMapper;
import scm.tenant.service.ITenantConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户配置表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class TenantConfigServiceImpl extends ServiceImpl<TenantConfigMapper, TenantConfig> implements ITenantConfigService {

}
