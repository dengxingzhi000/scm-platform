package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantOperationLog;
import scm.tenant.service.impl.TenantOperationLogServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tenant-operation-log")
@Tag(name = "租户操作日志管理", description = "租户操作日志的增删改查接口")
public class TenantOperationLogController {

    @Autowired
    private TenantOperationLogServiceImpl tenantOperationLogService;

    @PostMapping
    @Operation(summary = "创建租户操作日志")
    public TenantOperationLog create(@RequestBody TenantOperationLog entity) {
        log.info("[API] 创建租户操作日志: tenantId={}, operationType={}", entity.getTenantId(), entity.getOperationType());
        return tenantOperationLogService.createLog(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询租户操作日志")
    public TenantOperationLog getById(@PathVariable String id) {
        log.info("[API] 查询租户操作日志: id={}", id);
        return tenantOperationLogService.getById(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除租户操作日志")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户操作日志: id={}", id);
        return tenantOperationLogService.deleteById(id);
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "根据租户ID查询操作日志")
    public List<TenantOperationLog> listByTenantId(@PathVariable String tenantId) {
        log.info("[API] 查询租户操作日志: tenantId={}", tenantId);
        return tenantOperationLogService.listByTenantId(tenantId);
    }

    @GetMapping("/operation-type/{operationType}")
    @Operation(summary = "根据操作类型查询操作日志")
    public List<TenantOperationLog> listByOperationType(@PathVariable String operationType) {
        log.info("[API] 查询操作类型日志: operationType={}", operationType);
        return tenantOperationLogService.listByOperationType(operationType);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询租户操作日志")
    public Page<TenantOperationLog> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "操作模块") @RequestParam(required = false) String operationModule) {
        log.info("[API] 分页查询租户操作日志: page={}, size={}", page, size);
        return tenantOperationLogService.pageQuery(page, size, tenantId, operationType, operationModule);
    }
}
