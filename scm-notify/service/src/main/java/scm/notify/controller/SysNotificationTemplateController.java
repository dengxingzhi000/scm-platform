package scm.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.notify.domain.entity.SysNotificationTemplate;
import scm.notify.service.impl.SysNotificationTemplateServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sys-notification-template")
@Tag(name = "通知模板管理", description = "通知模板的增删改查接口")
public class SysNotificationTemplateController {

    @Autowired
    private SysNotificationTemplateServiceImpl notificationTemplateService;

    @PostMapping
    @Operation(summary = "创建通知模板")
    public SysNotificationTemplate create(@RequestBody SysNotificationTemplate entity) {
        log.info("[API] 创建通知模板: templateCode={}, channel={}", entity.getTemplateCode(), entity.getChannel());
        return notificationTemplateService.createTemplate(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询通知模板")
    public SysNotificationTemplate getById(@PathVariable String id) {
        log.info("[API] 查询通知模板: id={}", id);
        return notificationTemplateService.getById(id);
    }

    @PutMapping
    @Operation(summary = "更新通知模板")
    public SysNotificationTemplate update(@RequestBody SysNotificationTemplate entity) {
        log.info("[API] 更新通知模板: id={}", entity.getId());
        return notificationTemplateService.updateTemplate(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知模板")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除通知模板: id={}", id);
        return notificationTemplateService.deleteById(id);
    }

    @GetMapping("/channel/{channel}")
    @Operation(summary = "根据渠道查询通知模板")
    public List<SysNotificationTemplate> listByChannel(@PathVariable String channel) {
        log.info("[API] 查询渠道通知模板: channel={}", channel);
        return notificationTemplateService.listByType(channel);
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用通知模板")
    public boolean enable(@PathVariable String id) {
        log.info("[API] 启用通知模板: id={}", id);
        return notificationTemplateService.enableTemplate(id);
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用通知模板")
    public boolean disable(@PathVariable String id) {
        log.info("[API] 禁用通知模板: id={}", id);
        return notificationTemplateService.disableTemplate(id);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询通知模板")
    public Page<SysNotificationTemplate> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "通知渠道") @RequestParam(required = false) String channel,
            @Parameter(description = "模板编码") @RequestParam(required = false) String templateCode,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        log.info("[API] 分页查询通知模板: page={}, size={}", page, size);
        return notificationTemplateService.pageQuery(page, size, channel, templateCode, status);
    }
}
