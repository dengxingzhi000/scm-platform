package com.scmcloud.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.scmcloud.common.domain.TenantId;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.common.util.UUIDv7Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * MyBatis-Plus 审计字段自动填充处理器
 *
 * <p>自动填充字段：
 * <ol>
 *   <li>id - UUIDv7（INSERT 时）</li>
 *   <li>tenant_id - 从 ThreadLocal 获取（INSERT 时）</li>
 *   <li>create_time - 当前时间（INSERT 时）</li>
 *   <li>create_by - 当前用户ID（INSERT 时）</li>
 *   <li>update_time - 当前时间（INSERT 和 UPDATE 时）</li>
 *   <li>update_by - 当前用户ID（UPDATE 时）</li>
 *   <li>deleted - false（INSERT 时）</li>
 * </ol>
 *
 * @author Claude Code
 * @since 2025-01-24
 * @version 1.1
 * @apiNote 1.1 修复匿名用户检测，精简 tenantId 填充逻辑，修复乱码注释
 */
@Slf4j
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("Start insert fill ...");

        this.strictInsertFill(metaObject, "id", UUID.class, UUIDv7Util.generate());

        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            Class<?> fieldType = metaObject.getGetterType("tenantId");
            if (TenantId.class.isAssignableFrom(fieldType)) {
                this.strictInsertFill(metaObject, "tenantId", TenantId.class, TenantId.from(tenantId));
            } else {
                this.strictInsertFill(metaObject, "tenantId", String.class, tenantId.toString());
            }
        } else {
            log.warn("Tenant ID is null when inserting, entity: {}", metaObject.getOriginalObject().getClass().getName());
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        // strictInsertFill 只认与字段类型匹配的值：业务实体普遍是 LocalDateTime，
        // 统一填 OffsetDateTime 会导致 create_time/update_time 为 null 并触发非空约束。
        this.fillInsertTime(metaObject, "createTime", now);

        fillCreateBy(metaObject);

        this.fillInsertTime(metaObject, "updateTime", now);

        fillUpdateBy(metaObject);

        this.strictInsertFill(metaObject, "deleted", Boolean.class, false);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("Start update fill ...");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.fillUpdateTime(metaObject, "updateTime", now);

        fillUpdateBy(metaObject);
    }

    /**
     * 时间字段的实际 Java 类型：analytics 的实体用 OffsetDateTime，其余业务实体用 LocalDateTime。
     */
    private void fillInsertTime(MetaObject metaObject, String fieldName, OffsetDateTime now) {
        if (isOffsetDateTime(metaObject, fieldName)) {
            this.strictInsertFill(metaObject, fieldName, OffsetDateTime.class, now);
        } else {
            // LocalDateTime 字段使用服务器本地时区（Asia/Shanghai），不能拿 UTC 直接转。
            this.strictInsertFill(metaObject, fieldName, LocalDateTime.class, LocalDateTime.now());
        }
    }

    private void fillUpdateTime(MetaObject metaObject, String fieldName, OffsetDateTime now) {
        if (isOffsetDateTime(metaObject, fieldName)) {
            this.strictUpdateFill(metaObject, fieldName, OffsetDateTime.class, now);
        } else {
            this.strictUpdateFill(metaObject, fieldName, LocalDateTime.class, LocalDateTime.now());
        }
    }

    private boolean isOffsetDateTime(MetaObject metaObject, String fieldName) {
        if (!metaObject.hasGetter(fieldName)) {
            return false;
        }
        Class<?> type = metaObject.getGetterType(fieldName);
        return type != null && OffsetDateTime.class.isAssignableFrom(type);
    }

    private void fillCreateBy(MetaObject metaObject) {
        UUID userId = getCurrentUserId();
        if (userId == null || !metaObject.hasGetter("createBy")) return;

        Object createByValue = metaObject.getValue("createBy");
        if (createByValue instanceof String) {
            this.strictInsertFill(metaObject, "createBy", String.class, userId.toString());
        } else {
            this.strictInsertFill(metaObject, "createBy", UUID.class, userId);
        }
    }

    private void fillUpdateBy(MetaObject metaObject) {
        UUID userId = getCurrentUserId();
        if (userId == null || !metaObject.hasGetter("updateBy")) return;

        Object updateByValue = metaObject.getValue("updateBy");
        if (updateByValue instanceof String) {
            this.strictInsertFill(metaObject, "updateBy", String.class, userId.toString());
        } else {
            this.strictInsertFill(metaObject, "updateBy", UUID.class, userId);
        }
    }

    /**
     * 获取当前登录用户ID
     * <p>优先通过反射获取 getUserId()，避免与 scm-common-security-core 循环依赖
     */
    private UUID getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal == null) {
                return null;
            }

            // 通过反射获取 SecurityUser.getUserId()，避免循环依赖
            try {
                var method = principal.getClass().getMethod("getUserId");
                Object value = method.invoke(principal);
                if (value instanceof UUID uuid) {
                    return uuid;
                }
                if (value != null) {
                    return UUID.fromString(value.toString());
                }
            } catch (NoSuchMethodException ignored) {
                // principal 没有 getUserId() 方法
            } catch (Exception e) {
                log.debug("Failed to invoke getUserId() via reflection: {}", e.getMessage());
            }

            // 回退：尝试将 name 解析为 UUID（如 JWT subject claim）
            String name = authentication.getName();
            if (name != null && !name.isBlank() && !"anonymousUser".equals(name)) {
                try {
                    return UUID.fromString(name);
                } catch (IllegalArgumentException ignored) {
                    // name 不是 UUID 格式
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get current user ID from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}
