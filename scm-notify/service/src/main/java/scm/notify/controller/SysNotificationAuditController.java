package scm.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.notify.domain.entity.SysNotificationAudit;
import scm.notify.service.impl.SysNotificationAuditServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sys-notification-audit")
@Tag(name = "通知审计管理", description = "通知发送审计的增删改查接口")
public class SysNotificationAuditController {

    @Autowired
    private SysNotificationAuditServiceImpl notificationAuditService;

    @PostMapping
    @Operation(summary = "创建通知审计")
    public SysNotificationAudit create(@RequestBody SysNotificationAudit entity) {
        log.info("[API] 创建通知审计: userId={}, channel={}", entity.getUserId(), entity.getChannel());
        return notificationAuditService.createAudit(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询通知审计")
    public SysNotificationAudit getById(@PathVariable String id) {
        log.info("[API] 查询通知审计: id={}", id);
        return notificationAuditService.getById(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知审计")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除通知审计: id={}", id);
        return notificationAuditService.deleteById(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询通知审计")
    public List<SysNotificationAudit> listByUserId(@PathVariable String userId) {
        log.info("[API] 查询用户通知审计: userId={}", userId);
        return notificationAuditService.listByUserId(userId);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "根据状态查询通知审计")
    public List<SysNotificationAudit> listByStatus(@PathVariable String status) {
        log.info("[API] 查询状态通知审计: status={}", status);
        return notificationAuditService.listByStatus(status);
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "发送通知")
    public boolean sendNotification(@PathVariable String id) {
        log.info("[API] 发送通知: id={}", id);
        return notificationAuditService.sendNotification(id);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询通知审计")
    public Page<SysNotificationAudit> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "通知渠道") @RequestParam(required = false) String channel,
            @Parameter(description = "发送状态") @RequestParam(required = false) String status) {
        log.info("[API] 分页查询通知审计: page={}, size={}", page, size);
        return notificationAuditService.pageQuery(page, size, userId, channel, status);
    }
}
