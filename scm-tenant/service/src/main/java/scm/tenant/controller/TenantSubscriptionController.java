package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantSubscription;
import scm.tenant.service.impl.TenantSubscriptionServiceImpl;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-subscription")
public class TenantSubscriptionController {

    private final TenantSubscriptionServiceImpl tenantSubscriptionService;

    @PostMapping
    public TenantSubscription create(@RequestBody TenantSubscription entity) {
        log.info("[API] 创建租户订阅: tenantId={}, packageId={}", entity.getTenantId(), entity.getPackageId());
        return tenantSubscriptionService.createSubscription(entity);
    }

    @GetMapping("/{id}")
    public TenantSubscription getById(@PathVariable String id) {
        log.info("[API] 查询租户订阅: id={}", id);
        return tenantSubscriptionService.getById(id);
    }

    @PutMapping
    public TenantSubscription update(@RequestBody TenantSubscription entity) {
        log.info("[API] 更新租户订阅: id={}", entity.getId());
        return tenantSubscriptionService.updateSubscription(entity);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户订阅: id={}", id);
        return tenantSubscriptionService.deleteById(id);
    }

    @PostMapping("/subscribe")
    public boolean subscribe(
            @RequestParam String tenantId,
            @RequestParam String packageId) {
        log.info("[API] 租户订阅: tenantId={}, packageId={}", tenantId, packageId);
        return tenantSubscriptionService.subscribe(tenantId, packageId);
    }

    @PostMapping("/{id}/unsubscribe")
    public boolean unsubscribe(@PathVariable String id) {
        log.info("[API] 取消租户订阅: id={}", id);
        return tenantSubscriptionService.unsubscribe(id);
    }

    @GetMapping("/tenant/{tenantId}")
    public List<TenantSubscription> listByTenantId(@PathVariable String tenantId) {
        log.info("[API] 查询租户订阅: tenantId={}", tenantId);
        return tenantSubscriptionService.listByTenantId(tenantId);
    }

    @GetMapping("/page")
    public Page<TenantSubscription> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) Integer status) {
        log.info("[API] 分页查询租户订阅: page={}, size={}", page, size);
        return tenantSubscriptionService.pageQuery(page, size, tenantId, status);
    }
}
