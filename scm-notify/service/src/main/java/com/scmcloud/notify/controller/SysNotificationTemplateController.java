package com.scmcloud.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.notify.domain.entity.SysNotificationTemplate;
import com.scmcloud.notify.service.ISysNotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/sys-notification-template")
@Tag(name = "通知模板", description = "提供通知模板的创建、查询、更新、启停用与分页接口。")
public class SysNotificationTemplateController {

    private final ISysNotificationTemplateService notificationTemplateService;

    @Operation(summary = "创建通知模板", description = "创建通知模板并指定模板编码与通知渠道。")
    @PostMapping
    public SysNotificationTemplate create(@RequestBody SysNotificationTemplate entity) {
        log.info("[API] 创建通知模板: templateCode={}, channel={}", entity.getTemplateCode(), entity.getChannel());
        return notificationTemplateService.createTemplate(entity);
    }

    @Operation(summary = "查询通知模板", description = "根据模板ID查询通知模板详情。")
    @GetMapping("/{id}")
    public SysNotificationTemplate getById(@Parameter(description = "模板ID") @PathVariable String id) {
        log.info("[API] 查询通知模板: id={}", id);
        return notificationTemplateService.getById(id);
    }

    @Operation(summary = "更新通知模板", description = "更新通知模板的内容与渠道配置。")
    @PutMapping
    public SysNotificationTemplate update(@RequestBody SysNotificationTemplate entity) {
        log.info("[API] 更新通知模板: id={}", entity.getId());
        return notificationTemplateService.updateTemplate(entity);
    }

    @Operation(summary = "删除通知模板", description = "根据模板ID删除通知模板。")
    @DeleteMapping("/{id}")
    public boolean deleteById(@Parameter(description = "模板ID") @PathVariable String id) {
        log.info("[API] 删除通知模板: id={}", id);
        return notificationTemplateService.deleteById(id);
    }

    @Operation(summary = "查询渠道通知模板", description = "按通知渠道查询可用的模板列表。")
    @GetMapping("/channel/{channel}")
    public List<SysNotificationTemplate> listByChannel(@Parameter(description = "通知渠道") @PathVariable String channel) {
        log.info("[API] 查询渠道通知模板: channel={}", channel);
        return notificationTemplateService.listByType(channel);
    }

    @Operation(summary = "启用通知模板", description = "将指定通知模板状态置为启用。")
    @PutMapping("/{id}/enable")
    public boolean enable(@Parameter(description = "模板ID") @PathVariable String id) {
        log.info("[API] 启用通知模板: id={}", id);
        return notificationTemplateService.enableTemplate(id);
    }

    @Operation(summary = "禁用通知模板", description = "将指定通知模板状态置为禁用。")
    @PutMapping("/{id}/disable")
    public boolean disable(@Parameter(description = "模板ID") @PathVariable String id) {
        log.info("[API] 禁用通知模板: id={}", id);
        return notificationTemplateService.disableTemplate(id);
    }

    @Operation(summary = "分页查询通知模板", description = "按渠道、模板编码与状态分页查询通知模板。")
    @GetMapping("/page")
    public Page<SysNotificationTemplate> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "通知渠道") @RequestParam(required = false) String channel,
            @Parameter(description = "模板编码") @RequestParam(required = false) String templateCode,
            @Parameter(description = "模板状态") @RequestParam(required = false) Integer status) {
        log.info("[API] 分页查询通知模板: page={}, size={}", page, size);
        return notificationTemplateService.pageQuery(page, size, channel, templateCode, status);
    }
}
