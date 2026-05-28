package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.Tenant;
import scm.tenant.service.impl.TenantServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tenant")
@Tag(name = "租户管理", description = "租户的增删改查接口")
public class TenantController {

    @Autowired
    private TenantServiceImpl tenantService;

    @PostMapping
    @Operation(summary = "创建租户")
    public Tenant create(@RequestBody Tenant entity) {
        log.info("[API] 创建租户: tenantCode={}, tenantName={}", entity.getTenantCode(), entity.getTenantName());
        return tenantService.createTenant(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询租户")
    public Tenant getById(@PathVariable String id) {
        log.info("[API] 查询租户: id={}", id);
        return tenantService.getById(id);
    }

    @PutMapping
    @Operation(summary = "更新租户")
    public Tenant update(@RequestBody Tenant entity) {
        log.info("[API] 更新租户: id={}", entity.getId());
        return tenantService.updateTenant(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除租户")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户: id={}", id);
        return tenantService.deleteById(id);
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用租户")
    public boolean enable(@PathVariable String id) {
        log.info("[API] 启用租户: id={}", id);
        return tenantService.enableTenant(id);
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用租户")
    public boolean disable(@PathVariable String id) {
        log.info("[API] 禁用租户: id={}", id);
        return tenantService.disableTenant(id);
    }

    @GetMapping("/active")
    @Operation(summary = "查询活跃租户")
    public List<Tenant> listActive() {
        log.info("[API] 查询活跃租户");
        return tenantService.listActive();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询租户")
    public Page<Tenant> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户名称") @RequestParam(required = false) String tenantName,
            @Parameter(description = "租户类型") @RequestParam(required = false) Integer tenantType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        log.info("[API] 分页查询租户: page={}, size={}", page, size);
        return tenantService.pageQuery(page, size, tenantName, tenantType, status);
    }
}
