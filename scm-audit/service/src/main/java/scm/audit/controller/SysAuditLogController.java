package scm.audit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.audit.domain.entity.SysAuditLog;
import scm.audit.service.impl.SysAuditLogServiceImpl;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/sys-audit-log")
public class SysAuditLogController {

    private final SysAuditLogServiceImpl auditLogService;

    @PostMapping
    public SysAuditLog create(@RequestBody SysAuditLog entity) {
        log.info("[API] 创建审计日志: userId={}, operationType={}, module={}",
                entity.getUserId(), entity.getOperationType(), entity.getOperationModule());
        return auditLogService.createLog(entity);
    }

    @GetMapping("/{id}")
    public SysAuditLog getById(@PathVariable String id) {
        log.info("[API] 查询审计日志: id={}", id);
        return auditLogService.getById(id);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除审计日志: id={}", id);
        return auditLogService.deleteById(id);
    }

    @GetMapping("/user/{userId}")
    public List<SysAuditLog> listByUserId(@PathVariable String userId) {
        log.info("[API] 查询用户审计日志: userId={}", userId);
        return auditLogService.listByUserId(userId);
    }

    @GetMapping("/module/{module}")
    public List<SysAuditLog> listByModule(@PathVariable String module) {
        log.info("[API] 查询模块审计日志: module={}", module);
        return auditLogService.listByModule(module);
    }

    @GetMapping("/business-type/{businessType}")
    public List<SysAuditLog> listByBusinessType(@PathVariable String businessType) {
        log.info("[API] 查询业务类型审计日志: businessType={}", businessType);
        return auditLogService.listByBusinessType(businessType);
    }

    @GetMapping("/page")
    public Page<SysAuditLog> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Integer riskLevel) {
        log.info("[API] 分页查询审计日志: page={}, size={}", page, size);
        return auditLogService.pageQuery(page, size, userId, operationType, module, businessType, riskLevel);
    }
}
