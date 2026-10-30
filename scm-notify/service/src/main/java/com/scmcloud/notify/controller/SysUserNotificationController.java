package com.scmcloud.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.notify.domain.entity.SysUserNotification;
import com.scmcloud.notify.service.ISysUserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户站内通知（消息中心）接口。
 *
 * <p>接收人由登录态决定，接口不再接受 userId，避免越权读取或标记他人通知。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/sys-user-notification")
@Tag(name = "站内通知", description = "消息中心的通知列表、未读数与已读操作。")
public class SysUserNotificationController {

    private final ISysUserNotificationService notificationService;

    @Operation(summary = "分页查询站内通知", description = "查询当前登录用户在通知类型与已读状态条件下的通知列表。")
    @GetMapping("/page")
    public ApiResponse<Page<SysUserNotification>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "通知类型：SYSTEM/ORDER/INVENTORY/APPROVAL/SECURITY") @RequestParam(required = false) String type,
            @Parameter(description = "是否已读") @RequestParam(required = false) Boolean isRead) {
        return ApiResponse.success(notificationService.pageList(page, size, type, isRead));
    }

    @Operation(summary = "查询未读通知数", description = "统计当前登录用户的未读通知数量。")
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.success(notificationService.unreadCount());
    }

    @Operation(summary = "标记通知已读", description = "将当前登录用户的一条通知标记为已读；他人的通知按不存在处理。")
    @PutMapping("/{id}/read")
    public ApiResponse<Boolean> markAsRead(@Parameter(description = "通知 ID") @PathVariable String id) {
        return ApiResponse.success(notificationService.markAsRead(id));
    }

    @Operation(summary = "全部标记已读", description = "将当前登录用户的全部未读通知标记为已读。")
    @PutMapping("/read-all")
    public ApiResponse<Boolean> markAllAsRead() {
        return ApiResponse.success(notificationService.markAllAsRead());
    }
}
