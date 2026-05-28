package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantSubscription;
import scm.tenant.service.impl.TenantSubscriptionServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tenant-subscription")
@Tag(name = "租户订阅管理", description = "租户订阅的增删改查接口")
public class TenantSubscriptionController {

    @Autowired
    private TenantSubscriptionServiceImpl tenantSubscriptionService;

    @PostMapping
    @Operation(summary = "创建租户订阅")
    public TenantSubscription create(@RequestBody TenantSubscription entity) {
        log.info("[API] 创建租户订阅: tenantId={}, packageId={}", entity.getTenantId(), entity.getPackageId());
        return tenantSubscriptionService.createSubscription(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询租户订阅")
    public TenantSubscription getById(@PathVariable String id) {
        log.info("[API] 查询租户订阅: id={}", id);
        return tenantSubscriptionService.getById(id);
    }

    @PutMapping
    @Operation(summary = "更新租户订阅")
    public TenantSubscription update(@RequestBody TenantSubscription entity) {
        log.info("[API] 更新租户订阅: id={}", entity.getId());
        return tenantSubscriptionService.updateSubscription(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除租户订阅")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户订阅: id={}", id);
        return tenantSubscriptionService.deleteById(id);
    }

    @PostMapping("/subscribe")
    @Operation(summary = "租户订阅套餐")
    public boolean subscribe(
            @Parameter(description = "租户ID", required = true) @RequestParam String tenantId,
            @Parameter(description = "套餐ID", required = true) @RequestParam String packageId) {
        log.info("[API] 租户订阅: tenantId={}, packageId={}", tenantId, packageId);
        return tenantSubscriptionService.subscribe(tenantId, packageId);
    }

    @PostMapping("/{id}/unsubscribe")
    @Operation(summary = "取消租户订阅")
    public boolean unsubscribe(@PathVariable String id) {
        log.info("[API] 取消租户订阅: id={}", id);
        return tenantSubscriptionService.unsubscribe(id);
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "根据租户ID查询订阅")
    public List<TenantSubscription> listByTenantId(@PathVariable String tenantId) {
        log.info("[API] 查询租户订阅: tenantId={}", tenantId);
        return tenantSubscriptionService.listByTenantId(tenantId);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询租户订阅")
    public Page<TenantSubscription> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        log.info("[API] 分页查询租户订阅: page={}, size={}", page, size);
        return tenantSubscriptionService.pageQuery(page, size, tenantId, status);
    }
}
