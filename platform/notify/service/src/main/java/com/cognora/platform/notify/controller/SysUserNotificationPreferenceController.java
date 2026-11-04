package com.cognora.platform.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.web.ownership.OwnershipGuard;
import com.cognora.platform.common.web.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import com.cognora.platform.notify.domain.entity.SysUserNotificationPreference;
import com.cognora.platform.notify.service.ISysUserNotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/sys-user-notification-preference")
@Tag(name = "通知偏好", description = "提供用户通知偏好的创建、查询、更新与分页接口。")
public class SysUserNotificationPreferenceController {

    private final ISysUserNotificationPreferenceService userNotificationPreferenceService;

    @Operation(summary = "创建通知偏好", description = "为当前登录用户创建通知偏好并指定通知类型与通知渠道。")
    @PostMapping
    public SysUserNotificationPreference create(@RequestBody SysUserNotificationPreference entity) {
        // The owner always comes from the authenticated principal, never from the request body.
        entity.setUserId(currentUserId());
        log.info("[API] 创建用户通知偏好: userId={}, notificationType={}, channel={}",
                (entity.getUserId() + "").replace("\r", "_").replace("\n", "_"), (entity.getNotificationType() + "").replace("\r", "_").replace("\n", "_"), (entity.getChannel() + "").replace("\r", "_").replace("\n", "_"));
        return userNotificationPreferenceService.createPreference(entity);
    }

    @Operation(summary = "查询通知偏好", description = "根据偏好ID查询当前登录用户的偏好详情。")
    @GetMapping("/{id}")
    public SysUserNotificationPreference getById(@Parameter(description = "偏好ID") @PathVariable String id) {
        SysUserNotificationPreference preference = userNotificationPreferenceService.getById(id);
        requireOwner(preference);
        log.info("[API] 查询用户通知偏好: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return preference;
    }

    @Operation(summary = "更新通知偏好", description = "更新当前登录用户的通知偏好配置。")
    @PutMapping
    public SysUserNotificationPreference update(@RequestBody SysUserNotificationPreference entity) {
        SysUserNotificationPreference existing = userNotificationPreferenceService.getById(entity.getId());
        requireOwner(existing);
        entity.setUserId(currentUserId());
        log.info("[API] 更新用户通知偏好: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return userNotificationPreferenceService.updatePreference(entity);
    }

    @Operation(summary = "删除通知偏好", description = "根据偏好ID删除当前登录用户的通知偏好。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "偏好ID") @PathVariable String id) {
        SysUserNotificationPreference existing = userNotificationPreferenceService.getById(id);
        requireOwner(existing);
        log.info("[API] 删除用户通知偏好: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return userNotificationPreferenceService.deleteById(id);
    }

    @Operation(summary = "查询用户通知偏好", description = "查询当前登录用户的全部通知偏好。")
    @GetMapping("/user/{userId}")
    public List<SysUserNotificationPreference> getByUserId(@Parameter(description = "用户ID") @PathVariable String userId) {
        OwnershipGuard.requireSelfOrThrow(userId);
        log.info("[API] 查询用户通知偏好: userId={}", (userId + "").replace("\r", "_").replace("\n", "_"));
        return userNotificationPreferenceService.getByUserId(userId);
    }

    @Operation(summary = "分页查询通知偏好", description = "按通知类型与渠道分页查询当前登录用户的通知偏好。")
    @GetMapping("/page")
    public Page<SysUserNotificationPreference> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户ID（已废弃，以登录态为准）") @RequestParam(required = false) String userId,
            @Parameter(description = "通知类型") @RequestParam(required = false) String notificationType,
            @Parameter(description = "通知渠道") @RequestParam(required = false) String channel) {
        String currentUserId = currentUserId();
        log.info("[API] 分页查询用户通知偏好: page={}, size={}", page, size);
        return userNotificationPreferenceService.pageQuery(page, size, currentUserId, notificationType, channel);
    }

    private String currentUserId() {
        return SecurityUtils.requireCurrentUserUuid().toString();
    }

    private void requireOwner(SysUserNotificationPreference preference) {
        if (preference == null) {
            throw new AccessDeniedException("Access denied: preference not found");
        }
        OwnershipGuard.requireSelfOrThrow(preference.getUserId());
    }
}
