package scm.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.notify.domain.entity.SysNotificationAudit;
import scm.notify.service.impl.SysNotificationAuditServiceImpl;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/sys-notification-audit")
public class SysNotificationAuditController {

    private final SysNotificationAuditServiceImpl notificationAuditService;

    @PostMapping
    public SysNotificationAudit create(@RequestBody SysNotificationAudit entity) {
        log.info("[API] 创建通知审计: userId={}, channel={}", entity.getUserId(), entity.getChannel());
        return notificationAuditService.createAudit(entity);
    }

    @GetMapping("/{id}")
    public SysNotificationAudit getById(@PathVariable String id) {
        log.info("[API] 查询通知审计: id={}", id);
        return notificationAuditService.getById(id);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除通知审计: id={}", id);
        return notificationAuditService.deleteById(id);
    }

    @GetMapping("/user/{userId}")
    public List<SysNotificationAudit> listByUserId(@PathVariable String userId) {
        log.info("[API] 查询用户通知审计: userId={}", userId);
        return notificationAuditService.listByUserId(userId);
    }

    @GetMapping("/status/{status}")
    public List<SysNotificationAudit> listByStatus(@PathVariable String status) {
        log.info("[API] 查询状态通知审计: status={}", status);
        return notificationAuditService.listByStatus(status);
    }

    @PostMapping("/{id}/send")
    public boolean sendNotification(@PathVariable String id) {
        log.info("[API] 发送通知: id={}", id);
        return notificationAuditService.sendNotification(id);
    }

    @GetMapping("/page")
    public Page<SysNotificationAudit> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status) {
        log.info("[API] 分页查询通知审计: page={}, size={}", page, size);
        return notificationAuditService.pageQuery(page, size, userId, channel, status);
    }
}
