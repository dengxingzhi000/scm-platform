package scm.tenant.service.impl;

import scm.tenant.domain.entity.TenantPackage;
import scm.tenant.mapper.TenantPackageMapper;
import scm.tenant.service.ITenantPackageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户套餐表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class TenantPackageServiceImpl extends ServiceImpl<TenantPackageMapper, TenantPackage> implements ITenantPackageService {

}
