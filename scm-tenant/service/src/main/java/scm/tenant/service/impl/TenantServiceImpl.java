package scm.tenant.service.impl;

import scm.tenant.domain.entity.Tenant;
import scm.tenant.mapper.TenantMapper;
import scm.tenant.service.ITenantService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements ITenantService {

}
