package com.cognora.platform.system.service.support;

import com.cognora.platform.common.constant.RoleConstants;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.common.web.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 平台超级管理员判定。
 *
 * <p>平台级配置只能由平台运营方修改。判定口径优先级：</p>
 * <ol>
 *   <li>当前用户 ID 等于 {@code app.security.admin-user-id}（部署时配置的平台管理员）；</li>
 *   <li>当前用户持有 {@code ROLE_PLATFORM_ADMIN} 角色。</li>
 * </ol>
 *
 * <p>刻意不把 {@code ROLE_SUPER_ADMIN} 视作平台管理员：该角色在初始化数据里是租户内的
 * 超级管理员，放行会导致租户管理员越权修改平台级配置。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Component
@RequiredArgsConstructor
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
