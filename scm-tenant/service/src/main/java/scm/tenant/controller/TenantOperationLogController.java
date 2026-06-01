package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantOperationLog;
import scm.tenant.service.impl.TenantOperationLogServiceImpl;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/tenant-operation-log")
public class TenantOperationLogController {
    private final TenantOperationLogServiceImpl tenantOperationLogService;

    @PostMapping
    public TenantOperationLog create(@RequestBody TenantOperationLog entity) {
        log.info("[API] 创建租户操作日志: tenantId={}, operationType={}", entity.getTenantId(), entity.getOperationType());
        return tenantOperationLogService.createLog(entity);
    }

    @GetMapping("/{id}")
    public TenantOperationLog getById(@PathVariable String id) {
        log.info("[API] 查询租户操作日志: id={}", id);
        return tenantOperationLogService.getById(id);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户操作日志: id={}", id);
        return tenantOperationLogService.deleteById(id);
    }

    @GetMapping("/tenant/{tenantId}")
    public List<TenantOperationLog> listByTenantId(@PathVariable String tenantId) {
        log.info("[API] 查询租户操作日志: tenantId={}", tenantId);
        return tenantOperationLogService.listByTenantId(tenantId);
    }

    @GetMapping("/operation-type/{operationType}")
    public List<TenantOperationLog> listByOperationType(@PathVariable String operationType) {
        log.info("[API] 查询操作类型日志: operationType={}", operationType);
        return tenantOperationLogService.listByOperationType(operationType);
    }

    @GetMapping("/page")
    public Page<TenantOperationLog> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operationModule) {
        log.info("[API] 分页查询租户操作日志: page={}, size={}", page, size);
        return tenantOperationLogService.pageQuery(page, size, tenantId, operationType, operationModule);
    }
}
