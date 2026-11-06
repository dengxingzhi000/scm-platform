package com.cognora.platform.tenant.support;

import com.cognora.platform.common.constant.RoleConstants;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.common.web.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 平台超级管理员判定（与 scm-system 的 {@code PlatformAdminGuard} 口径一致）。
 *
 * <p>平台级配置（{@code tenant_id IS NULL}）只能由平台运营方读写。判定优先级：</p>
 * <ol>
 *   <li>当前用户 ID 等于 {@code app.security.admin-user-id}；</li>
 *   <li>当前用户持有 {@code ROLE_PLATFORM_ADMIN} 角色。</li>
 * </ol>
 *
 * <p>刻意不把 {@code ROLE_SUPER_ADMIN} 视作平台管理员：该角色是租户内的超级管理员，
 * 放行会导致租户管理员越权访问平台级配置。</p>
 */
@Component
public class PlatformAdminGuard {

    @Value("${app.security.admin-user-id:}")
    private String adminUserId;

    public boolean isPlatformAdmin() {
        UUID currentUserId = SecurityUtils.getCurrentUserUuid().orElse(null);
        if (currentUserId == null) {
            return false;
        }
        if (StringUtils.hasText(adminUserId)
                && adminUserId.trim().equalsIgnoreCase(currentUserId.toString())) {
            return true;
        }
        SecurityUser currentUser = SecurityUtils.getCurrentUser();
        return currentUser != null
                && currentUser.getRoles() != null
                && currentUser.getRoles().contains(RoleConstants.ROLE_PLATFORM_ADMIN);
    }
}
