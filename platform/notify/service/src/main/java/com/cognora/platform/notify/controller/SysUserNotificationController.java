package com.cognora.platform.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.notify.domain.entity.SysUserNotification;
import com.cognora.platform.notify.service.ISysUserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户站内通知（消息中心）接口。
 *
 * <p>接收人由登录态决定，接口不再接受 userId，避免越权读取或标记他人通知。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/sys-user-notification")
@Tag(name = "站内通知", description = "消息中心的通知列表、未读数、已读与删除操作。")
public class SysUserNotificationController {

    /** One page of selections is the realistic upper bound; a bigger list would also blow past PG's parameter limit. */
    private static final int MAX_BATCH_DELETE = 200;

    private final ISysUserNotificationService notificationService;

    @Operation(summary = "分页查询站内通知",
            description = "查询当前登录用户在类型、已读状态、关键字与创建时间范围内的通知列表。")
    @GetMapping("/page")
    public ApiResponse<Page<SysUserNotification>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "通知类型：SYSTEM/ORDER/INVENTORY/APPROVAL/SECURITY") @RequestParam(required = false) String type,
            @Parameter(description = "是否已读") @RequestParam(required = false) Boolean isRead,
            @Parameter(description = "标题或内容关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "创建时间起（ISO-8601，含）")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "创建时间止（ISO-8601，含）")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.success(
                notificationService.pageList(page, size, type, isRead, keyword, startTime, endTime));
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

    @Operation(summary = "删除通知", description = "逻辑删除当前登录用户的一条通知；他人的通知按不存在处理。")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@Parameter(description = "通知 ID") @PathVariable String id) {
        return ApiResponse.success(notificationService.deleteNotification(id));
    }

    @Operation(summary = "批量删除通知", description = "逻辑删除当前登录用户选中的通知，返回实际删除条数。")
    @DeleteMapping
    public ApiResponse<Integer> deleteBatch(@Parameter(description = "通知 ID，逗号分隔或重复传参") @RequestParam("ids") List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.success(0);
        }
        if (ids.size() > MAX_BATCH_DELETE) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST,
                    "一次最多删除 " + MAX_BATCH_DELETE + " 条通知");
        }
        return ApiResponse.success(notificationService.deleteNotifications(ids));
    }
}
