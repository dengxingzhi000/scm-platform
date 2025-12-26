package scm.tenant.service.impl;

import scm.tenant.domain.entity.TenantResourceQuota;
import scm.tenant.mapper.TenantResourceQuotaMapper;
import scm.tenant.service.ITenantResourceQuotaService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户资源配额表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class TenantResourceQuotaServiceImpl extends ServiceImpl<TenantResourceQuotaMapper, TenantResourceQuota> implements ITenantResourceQuotaService {

}
