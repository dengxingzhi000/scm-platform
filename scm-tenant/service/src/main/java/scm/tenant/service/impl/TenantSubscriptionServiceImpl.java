package scm.tenant.service.impl;

import scm.tenant.domain.entity.TenantSubscription;
import scm.tenant.mapper.TenantSubscriptionMapper;
import scm.tenant.service.ITenantSubscriptionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户订阅表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class TenantSubscriptionServiceImpl extends ServiceImpl<TenantSubscriptionMapper, TenantSubscription> implements ITenantSubscriptionService {

}
