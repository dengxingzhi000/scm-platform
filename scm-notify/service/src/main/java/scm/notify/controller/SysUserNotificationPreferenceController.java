package scm.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.notify.domain.entity.SysUserNotificationPreference;
import scm.notify.service.impl.SysUserNotificationPreferenceServiceImpl;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/sys-user-notification-preference")
public class SysUserNotificationPreferenceController {

    private final SysUserNotificationPreferenceServiceImpl userNotificationPreferenceService;

    @PostMapping
    public SysUserNotificationPreference create(@RequestBody SysUserNotificationPreference entity) {
        log.info("[API] 创建用户通知偏好: userId={}, notificationType={}, channel={}",
                entity.getUserId(), entity.getNotificationType(), entity.getChannel());
        return userNotificationPreferenceService.createPreference(entity);
    }

    @GetMapping("/{id}")
    public SysUserNotificationPreference getById(@PathVariable String id) {
        log.info("[API] 查询用户通知偏好: id={}", id);
        return userNotificationPreferenceService.getById(id);
    }

    @PutMapping
    public SysUserNotificationPreference update(@RequestBody SysUserNotificationPreference entity) {
        log.info("[API] 更新用户通知偏好: id={}", entity.getId());
        return userNotificationPreferenceService.updatePreference(entity);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除用户通知偏好: id={}", id);
        return userNotificationPreferenceService.deleteById(id);
    }

    @GetMapping("/user/{userId}")
    public List<SysUserNotificationPreference> getByUserId(@PathVariable String userId) {
        log.info("[API] 查询用户通知偏好: userId={}", userId);
        return userNotificationPreferenceService.getByUserId(userId);
    }

    @GetMapping("/page")
    public Page<SysUserNotificationPreference> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) String channel) {
        log.info("[API] 分页查询用户通知偏好: page={}, size={}", page, size);
        return userNotificationPreferenceService.pageQuery(page, size, userId, notificationType, channel);
    }
}
