package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.Tenant;
import scm.tenant.service.impl.TenantServiceImpl;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant")
public class TenantController {

    private final TenantServiceImpl tenantService;

    @PostMapping
    public Tenant create(@RequestBody Tenant entity) {
        log.info("[API] 创建租户: tenantCode={}, tenantName={}", entity.getTenantCode(), entity.getTenantName());
        return tenantService.createTenant(entity);
    }

    @GetMapping("/{id}")
    public Tenant getById(@PathVariable String id) {
        log.info("[API] 查询租户: id={}", id);
        return tenantService.getById(id);
    }

    @PutMapping
    public Tenant update(@RequestBody Tenant entity) {
        log.info("[API] 更新租户: id={}", entity.getId());
        return tenantService.updateTenant(entity);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户: id={}", id);
        return tenantService.deleteById(id);
    }

    @PutMapping("/{id}/enable")
    public boolean enable(@PathVariable String id) {
        log.info("[API] 启用租户: id={}", id);
        return tenantService.enableTenant(id);
    }

    @PutMapping("/{id}/disable")
    public boolean disable(@PathVariable String id) {
        log.info("[API] 禁用租户: id={}", id);
        return tenantService.disableTenant(id);
    }

    @GetMapping("/active")
    public List<Tenant> listActive() {
        log.info("[API] 查询活跃租户");
        return tenantService.listActive();
    }

    @GetMapping("/page")
    public Page<Tenant> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tenantName,
            @RequestParam(required = false) Integer tenantType,
            @RequestParam(required = false) Integer status) {
        log.info("[API] 分页查询租户: page={}, size={}", page, size);
        return tenantService.pageQuery(page, size, tenantName, tenantType, status);
    }
}
