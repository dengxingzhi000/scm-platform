package scm.audit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.audit.domain.entity.SysAuditLog;
import scm.audit.service.impl.SysAuditLogServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sys-audit-log")
@Tag(name = "审计日志管理", description = "操作审计日志的增删改查接口")
public class SysAuditLogController {

    @Autowired
    private SysAuditLogServiceImpl auditLogService;

    @PostMapping
    @Operation(summary = "创建审计日志")
    public SysAuditLog create(@RequestBody SysAuditLog entity) {
        log.info("[API] 创建审计日志: userId={}, operationType={}, module={}",
                entity.getUserId(), entity.getOperationType(), entity.getOperationModule());
        return auditLogService.createLog(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询审计日志")
    public SysAuditLog getById(@PathVariable String id) {
        log.info("[API] 查询审计日志: id={}", id);
        return auditLogService.getById(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除审计日志")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除审计日志: id={}", id);
        return auditLogService.deleteById(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询审计日志")
    public List<SysAuditLog> listByUserId(@PathVariable String userId) {
        log.info("[API] 查询用户审计日志: userId={}", userId);
        return auditLogService.listByUserId(userId);
    }

    @GetMapping("/module/{module}")
    @Operation(summary = "根据模块查询审计日志")
    public List<SysAuditLog> listByModule(@PathVariable String module) {
        log.info("[API] 查询模块审计日志: module={}", module);
        return auditLogService.listByModule(module);
    }

    @GetMapping("/business-type/{businessType}")
    @Operation(summary = "根据业务类型查询审计日志")
    public List<SysAuditLog> listByBusinessType(@PathVariable String businessType) {
        log.info("[API] 查询业务类型审计日志: businessType={}", businessType);
        return auditLogService.listByBusinessType(businessType);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询审计日志")
    public Page<SysAuditLog> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "模块") @RequestParam(required = false) String module,
            @Parameter(description = "业务类型") @RequestParam(required = false) String businessType,
            @Parameter(description = "风险等级") @RequestParam(required = false) Integer riskLevel) {
        log.info("[API] 分页查询审计日志: page={}, size={}", page, size);
        return auditLogService.pageQuery(page, size, userId, operationType, module, businessType, riskLevel);
    }
}
