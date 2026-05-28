package scm.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.notify.domain.entity.SysUserNotificationPreference;
import scm.notify.service.impl.SysUserNotificationPreferenceServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sys-user-notification-preference")
@Tag(name = "用户通知偏好管理", description = "用户通知偏好的增删改查接口")
public class SysUserNotificationPreferenceController {

    @Autowired
    private SysUserNotificationPreferenceServiceImpl userNotificationPreferenceService;

    @PostMapping
    @Operation(summary = "创建用户通知偏好")
    public SysUserNotificationPreference create(@RequestBody SysUserNotificationPreference entity) {
        log.info("[API] 创建用户通知偏好: userId={}, notificationType={}, channel={}",
                entity.getUserId(), entity.getNotificationType(), entity.getChannel());
        return userNotificationPreferenceService.createPreference(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户通知偏好")
    public SysUserNotificationPreference getById(@PathVariable String id) {
        log.info("[API] 查询用户通知偏好: id={}", id);
        return userNotificationPreferenceService.getById(id);
    }

    @PutMapping
    @Operation(summary = "更新用户通知偏好")
    public SysUserNotificationPreference update(@RequestBody SysUserNotificationPreference entity) {
        log.info("[API] 更新用户通知偏好: id={}", entity.getId());
        return userNotificationPreferenceService.updatePreference(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户通知偏好")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除用户通知偏好: id={}", id);
        return userNotificationPreferenceService.deleteById(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询通知偏好")
    public List<SysUserNotificationPreference> getByUserId(@PathVariable String userId) {
        log.info("[API] 查询用户通知偏好: userId={}", userId);
        return userNotificationPreferenceService.getByUserId(userId);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询用户通知偏好")
    public Page<SysUserNotificationPreference> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "通知类型") @RequestParam(required = false) String notificationType,
            @Parameter(description = "通知渠道") @RequestParam(required = false) String channel) {
        log.info("[API] 分页查询用户通知偏好: page={}, size={}", page, size);
        return userNotificationPreferenceService.pageQuery(page, size, userId, notificationType, channel);
    }
}
