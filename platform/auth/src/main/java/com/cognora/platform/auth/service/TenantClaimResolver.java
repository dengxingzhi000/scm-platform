package com.cognora.platform.auth.service;

import com.cognora.platform.tenant.api.dto.tenant.TenantDTO;
import com.cognora.platform.tenant.api.service.TenantDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 登录/WebAuthn 签发 Token 时解析租户编码。
 *
 * <p>安全判定以 token 里的 tenantId 为准，tenantCode 只用于展示与排障。
 * 因此租户服务不可达时不阻断登录，只降级为“token 仅带 tenantId”，
 * 并记录告警。若后续要求 tenantCode 强一致，可在此改为抛异常。</p>
 */
@Component
@Slf4j
public class TenantClaimResolver {

    @DubboReference(check = false)
    private TenantDubboService tenantDubboService;

    public String resolveTenantCode(UUID tenantId) {
        if (tenantId == null) {
            return null;
        }
        try {
            TenantDTO tenant = tenantDubboService.getById(tenantId.toString());
            if (tenant != null && tenant.getTenantCode() != null && !tenant.getTenantCode().isBlank()) {
                return tenant.getTenantCode();
            }
            log.warn("Tenant {} has no tenantCode, token will carry tenantId only", tenantId);
        } catch (Exception e) {
            log.warn("Cannot resolve tenantCode for tenant {}: {}; token will carry tenantId only",
                    tenantId, e.getMessage());
        }
        return null;
    }
}
