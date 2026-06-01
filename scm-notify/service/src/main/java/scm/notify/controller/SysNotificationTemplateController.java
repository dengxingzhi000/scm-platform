package scm.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.notify.domain.entity.SysNotificationTemplate;
import scm.notify.service.impl.SysNotificationTemplateServiceImpl;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/sys-notification-template")
public class SysNotificationTemplateController {

    private final SysNotificationTemplateServiceImpl notificationTemplateService;

    @PostMapping
    public SysNotificationTemplate create(@RequestBody SysNotificationTemplate entity) {
        log.info("[API] 创建通知模板: templateCode={}, channel={}", entity.getTemplateCode(), entity.getChannel());
        return notificationTemplateService.createTemplate(entity);
    }

    @GetMapping("/{id}")
    public SysNotificationTemplate getById(@PathVariable String id) {
        log.info("[API] 查询通知模板: id={}", id);
        return notificationTemplateService.getById(id);
    }

    @PutMapping
    public SysNotificationTemplate update(@RequestBody SysNotificationTemplate entity) {
        log.info("[API] 更新通知模板: id={}", entity.getId());
        return notificationTemplateService.updateTemplate(entity);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除通知模板: id={}", id);
        return notificationTemplateService.deleteById(id);
    }

    @GetMapping("/channel/{channel}")
    public List<SysNotificationTemplate> listByChannel(@PathVariable String channel) {
        log.info("[API] 查询渠道通知模板: channel={}", channel);
        return notificationTemplateService.listByType(channel);
    }

    @PutMapping("/{id}/enable")
    public boolean enable(@PathVariable String id) {
        log.info("[API] 启用通知模板: id={}", id);
        return notificationTemplateService.enableTemplate(id);
    }

    @PutMapping("/{id}/disable")
    public boolean disable(@PathVariable String id) {
        log.info("[API] 禁用通知模板: id={}", id);
        return notificationTemplateService.disableTemplate(id);
    }

    @GetMapping("/page")
    public Page<SysNotificationTemplate> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) Integer status) {
        log.info("[API] 分页查询通知模板: page={}, size={}", page, size);
        return notificationTemplateService.pageQuery(page, size, channel, templateCode, status);
    }
}
