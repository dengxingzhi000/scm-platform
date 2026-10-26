package com.scmcloud.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.notify.domain.entity.SysNotificationAudit;
import com.scmcloud.notify.service.ISysNotificationAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/sys-notification-audit")
@Tag(name = "通知审计", description = "提供通知审计记录的创建、查询、分页与发送接口。")
public class SysNotificationAuditController {

    private final ISysNotificationAuditService notificationAuditService;

    @Operation(summary = "创建通知审计", description = "创建一条通知审计记录。")
    @PostMapping
    public SysNotificationAudit create(@RequestBody SysNotificationAudit entity) {
        log.info("[API] 创建通知审计: userId={}, channel={}", entity.getUserId(), entity.getChannel());
        return notificationAuditService.createAudit(entity);
    }

    @Operation(summary = "查询通知审计", description = "根据审计记录ID查询通知审计详情。")
    @GetMapping("/{id}")
    public SysNotificationAudit getById(@Parameter(description = "审计记录ID") @PathVariable String id) {
        log.info("[API] 查询通知审计: id={}", id);
        return notificationAuditService.getById(id);
    }

    @Operation(summary = "删除通知审计", description = "根据审计记录ID删除通知审计记录。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "审计记录ID") @PathVariable String id) {
        log.info("[API] 删除通知审计: id={}", id);
        return notificationAuditService.deleteById(id);
    }

    @Operation(summary = "查询用户通知审计", description = "查询指定用户的全部通知审计记录。")
    @GetMapping("/user/{userId}")
    public List<SysNotificationAudit> listByUserId(@Parameter(description = "用户ID") @PathVariable String userId) {
        log.info("[API] 查询用户通知审计: userId={}", userId);
        return notificationAuditService.listByUserId(userId);
    }

    @Operation(summary = "按状态查询通知审计", description = "按通知状态查询通知审计记录。")
    @GetMapping("/status/{status}")
    public List<SysNotificationAudit> listByStatus(@Parameter(description = "通知状态") @PathVariable String status) {
        log.info("[API] 查询状态通知审计: status={}", status);
        return notificationAuditService.listByStatus(status);
    }

    @Operation(summary = "发送通知", description = "根据审计记录ID触发对应通知的发送。")
    @PostMapping("/{id}/send")
    public boolean sendNotification(@Parameter(description = "审计记录ID") @PathVariable String id) {
        log.info("[API] 发送通知: id={}", id);
        return notificationAuditService.sendNotification(id);
    }

    @Operation(summary = "分页查询通知审计", description = "按用户、渠道与状态分页查询通知审计记录。")
    @GetMapping("/page")
    public Page<SysNotificationAudit> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "通知渠道") @RequestParam(required = false) String channel,
            @Parameter(description = "通知状态") @RequestParam(required = false) String status) {
        log.info("[API] 分页查询通知审计: page={}, size={}", page, size);
        return notificationAuditService.pageQuery(page, size, userId, channel, status);
    }
}
