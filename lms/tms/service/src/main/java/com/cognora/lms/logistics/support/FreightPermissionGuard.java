package com.cognora.lms.logistics.support;

import com.cognora.platform.common.security.PermissionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 物流高敏操作的权限校验（批次 3）。
 *
 * <p>权限码命名沿用平台 {@code 域:资源:动作} 风格，当前先落 {@code lms:freight:reconciliation:confirm}，
 * 待 platform/system 权限管理模块确认最终编码后可平移。</p>
 */
@Component
@RequiredArgsConstructor
public class FreightPermissionGuard {

    /** 运费对账差异确认权限码。 */
    public static final String RECONCILIATION_CONFIRM = "lms:freight:reconciliation:confirm";

    private final ObjectProvider<PermissionChecker> permissionCheckerProvider;

    public void requireReconciliationConfirmPermission() {
        PermissionChecker checker = permissionCheckerProvider.getIfAvailable();
        if (checker == null) {
            // 权限组件未启用（security.permission-checker.enabled=false）时保持原有部署可用性。
            return;
        }
        UUID userId = CurrentUserSupport.currentUserId();
        checker.requirePermission(userId, RECONCILIATION_CONFIRM);
    }
}
