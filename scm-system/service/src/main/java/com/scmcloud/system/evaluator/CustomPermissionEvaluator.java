package com.scmcloud.system.evaluator;

import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.system.service.ISysPermissionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 鑷畾涔夋潈闄愯瘎浼板櫒
 *
 * @author Deng
 * createData 2025/10/14 14:59
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {
    private final ISysPermissionService permissionService;

    /**
     * 鍒ゆ柇鐢ㄦ埛鏄惁鏈夋寚瀹氭潈锟?
     */
    @Override
    public boolean hasPermission(@NonNull Authentication authentication, @NonNull Object targetDomainObject,
                                 @NonNull Object permission) {
        if (!(authentication.getPrincipal() instanceof SecurityUser user)) {
            return false;
        }

        String permissionCode = permission.toString();

        // 妫€鏌ョ敤鎴锋槸鍚︽湁璇ユ潈锟?
        boolean hasPermission = permissionService.hasPermission(user.getUserId(), permissionCode);

        log.debug("Permission check - User: {}, Permission: {}, Result: {}",
                user.getUsername(), permissionCode, hasPermission);

        return hasPermission;
    }

    /**
     * 鍒ゆ柇鐢ㄦ埛鏄惁鏈夋寚瀹氳祫婧愮殑鏉冮檺锛堝熀浜庤祫婧怚D锟?
     */
    @Override
    public boolean hasPermission(@NonNull Authentication authentication, @NonNull Serializable targetId,
                                 @NonNull String targetType, @NonNull Object permission) {
        if (!(authentication.getPrincipal() instanceof SecurityUser user)) {
            return false;
        }

        // 鍙互瀹炵幇鏇村鏉傜殑璧勬簮绾ф潈闄愭帶锟?
        // 渚嬪锛氭鏌ョ敤鎴锋槸鍚﹀彲浠ヨ闂壒瀹欼D鐨勮祫锟?
        boolean hasPermission = permissionService.hasResourcePermission(
                user.getUserId(), targetType, targetId, permission.toString());

        log.debug("Resource permission check - User: {}, TargetType: {}, TargetId: {}, Permission: {}, Result: {}",
                user.getUsername(), targetType, targetId, permission, hasPermission);

        return hasPermission;
    }
}

