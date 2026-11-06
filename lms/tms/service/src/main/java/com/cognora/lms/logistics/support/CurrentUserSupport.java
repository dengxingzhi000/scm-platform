package com.cognora.lms.logistics.support;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * 当前登录用户解析（批次 3）。
 *
 * <p>仓储层不使用此类，仅接口层做权限校验时读取认证主体；
 * 兼容 principal 直接是 UUID 或带 {@code getUserId()} 的安全用户对象。</p>
 */
public final class CurrentUserSupport {

    private CurrentUserSupport() {
    }

    public static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID uuid) {
            return uuid;
        }
        try {
            Object value = principal.getClass().getMethod("getUserId").invoke(principal);
            if (value instanceof UUID uuid) {
                return uuid;
            }
            if (value instanceof String text && !text.isBlank()) {
                return UUID.fromString(text);
            }
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            // 非平台安全用户主体（如匿名/系统任务），按未登录处理。
        }
        return null;
    }
}
