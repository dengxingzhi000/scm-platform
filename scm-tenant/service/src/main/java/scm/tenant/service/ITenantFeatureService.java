package scm.tenant.service;

import scm.tenant.domain.entity.TenantFeature;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 租户功能开关表 服务类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface ITenantFeatureService extends IService<TenantFeature> {

    /**
     * 判断租户的某个功能是否启用。
     *
     * @param tenantId   租户ID
     * @param featureCode 功能编码
     * @return true=启用, false=禁用
     */
    boolean isFeatureEnabled(String tenantId, String featureCode);
}
