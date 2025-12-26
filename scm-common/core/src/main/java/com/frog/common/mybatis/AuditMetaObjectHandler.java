package com.frog.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.frog.common.tenant.TenantContextHolder;
import com.frog.common.util.UUIDv7Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * MyBatis-Plus 审计字段自动填充处理器
 *
 * 自动填充字段：
 * 1. id - UUIDv7（INSERT时）
 * 2. tenant_id - 从ThreadLocal获取（INSERT时）
 * 3. create_time - 当前时间（INSERT时）
 * 4. create_by - 当前用户ID（INSERT时）
 * 5. update_time - 当前时间（INSERT和UPDATE时）
 * 6. update_by - 当前用户ID（UPDATE时）
 * 7. deleted - false（INSERT时）
 *
 * 使用方式：
 * 实体类字段添加 @TableField 注解：
 * <pre>
 * @TableField(fill = FieldFill.INSERT)
 * private UUID id;
 *
 * @TableField(fill = FieldFill.INSERT)
 * private UUID tenantId;
 *
 * @TableField(fill = FieldFill.INSERT)
 * private OffsetDateTime createTime;
 *
 * @TableField(fill = FieldFill.INSERT_UPDATE)
 * private OffsetDateTime updateTime;
 * </pre>
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("Start insert fill ...");

        // 1. 自动填充 id（UUIDv7）
        this.strictInsertFill(metaObject, "id", UUID.class, UUIDv7Util.generate());

        // 2. 自动填充 tenant_id
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            this.strictInsertFill(metaObject, "tenantId", UUID.class, tenantId);
        } else {
            log.warn("Tenant ID is null when inserting, entity: {}", metaObject.getOriginalObject().getClass().getName());
        }

        // 3. 自动填充 create_time
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.strictInsertFill(metaObject, "createTime", OffsetDateTime.class, now);

        // 4. 自动填充 create_by（需要从SecurityContext或其他地方获取当前用户）
        UUID currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            this.strictInsertFill(metaObject, "createBy", UUID.class, currentUserId);
        }

        // 5. 自动填充 update_time
        this.strictInsertFill(metaObject, "updateTime", OffsetDateTime.class, now);

        // 6. 自动填充 update_by
        if (currentUserId != null) {
            this.strictInsertFill(metaObject, "updateBy", UUID.class, currentUserId);
        }

        // 7. 自动填充 deleted（软删除标志）
        this.strictInsertFill(metaObject, "deleted", Boolean.class, false);
    }

    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("Start update fill ...");

        // 1. 自动填充 update_time
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.strictUpdateFill(metaObject, "updateTime", OffsetDateTime.class, now);

        // 2. 自动填充 update_by
        UUID currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            this.strictUpdateFill(metaObject, "updateBy", UUID.class, currentUserId);
        }
    }

    /**
     * 获取当前登录用户ID
     *
     * TODO: 从 Spring Security / JWT / Session 中获取当前用户ID
     * 这里提供默认实现，实际项目需要根据具体认证方式调整
     */
    private UUID getCurrentUserId() {
        // 方案1：从 SecurityContextHolder 获取（Spring Security）
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
        //     UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        //     return UUID.fromString(userDetails.getUsername()); // 假设username是UUID
        // }

        // 方案2：从自定义的 ThreadLocal 获取
        // return UserContextHolder.getUserId();

        // 方案3：从 JWT Token 中获取
        // return JwtUtil.getCurrentUserId();

        // 临时返回null（实际项目需要实现）
        return null;
    }
}