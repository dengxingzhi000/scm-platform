package com.scmcloud.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.notify.domain.entity.SysUserNotificationPreference;
import com.scmcloud.notify.service.ISysUserNotificationPreferenceService;
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

    @Operation(summary = "创建通知偏好", description = "创建用户通知偏好并指定通知类型与通知渠道。")
    @PostMapping
    public SysUserNotificationPreference create(@RequestBody SysUserNotificationPreference entity) {
        log.info("[API] 创建用户通知偏好: userId={}, notificationType={}, channel={}",
                (entity.getUserId() + "").replace("\r", "_").replace("\n", "_"), (entity.getNotificationType() + "").replace("\r", "_").replace("\n", "_"), (entity.getChannel() + "").replace("\r", "_").replace("\n", "_"));
        return userNotificationPreferenceService.createPreference(entity);
    }

    @Operation(summary = "查询通知偏好", description = "根据偏好ID查询用户通知偏好详情。")
    @GetMapping("/{id}")
    public SysUserNotificationPreference getById(@Parameter(description = "偏好ID") @PathVariable String id) {
        log.info("[API] 查询用户通知偏好: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return userNotificationPreferenceService.getById(id);
    }

    @Operation(summary = "更新通知偏好", description = "更新用户通知偏好配置。")
    @PutMapping
    public SysUserNotificationPreference update(@RequestBody SysUserNotificationPreference entity) {
        log.info("[API] 更新用户通知偏好: id={}", (entity.getId() + "").replace("\r", "_").replace("\n", "_"));
        return userNotificationPreferenceService.updatePreference(entity);
    }

    @Operation(summary = "删除通知偏好", description = "根据偏好ID删除用户通知偏好。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "偏好ID") @PathVariable String id) {
        log.info("[API] 删除用户通知偏好: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return userNotificationPreferenceService.deleteById(id);
    }

    @Operation(summary = "查询用户通知偏好", description = "查询指定用户的全部通知偏好。")
    @GetMapping("/user/{userId}")
    public List<SysUserNotificationPreference> getByUserId(@Parameter(description = "用户ID") @PathVariable String userId) {
        log.info("[API] 查询用户通知偏好: userId={}", (userId + "").replace("\r", "_").replace("\n", "_"));
        return userNotificationPreferenceService.getByUserId(userId);
    }

    @Operation(summary = "分页查询通知偏好", description = "按用户、通知类型与渠道分页查询用户通知偏好。")
    @GetMapping("/page")
    public Page<SysUserNotificationPreference> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "通知类型") @RequestParam(required = false) String notificationType,
            @Parameter(description = "通知渠道") @RequestParam(required = false) String channel) {
        log.info("[API] 分页查询用户通知偏好: page={}, size={}", page, size);
        return userNotificationPreferenceService.pageQuery(page, size, userId, notificationType, channel);
    }
}
