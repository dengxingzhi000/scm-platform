package scm.audit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.audit.domain.entity.SysSensitiveOperationLog;
import scm.audit.service.impl.SysSensitiveOperationLogServiceImpl;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/sys-sensitive-operation-log")
public class SysSensitiveOperationLogController {

    private final SysSensitiveOperationLogServiceImpl sensitiveOperationLogService;

    @PostMapping
    public SysSensitiveOperationLog create(@RequestBody SysSensitiveOperationLog entity) {
        log.info("[API] 创建敏感操作日志: userId={}, operationType={}", entity.getUserId(), entity.getOperationType());
        return sensitiveOperationLogService.createLog(entity);
    }

    @GetMapping("/{id}")
    public SysSensitiveOperationLog getById(@PathVariable String id) {
        log.info("[API] 查询敏感操作日志: id={}", id);
        return sensitiveOperationLogService.getById(id);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除敏感操作日志: id={}", id);
        return sensitiveOperationLogService.deleteById(id);
    }

    @GetMapping("/user/{userId}")
    public List<SysSensitiveOperationLog> listByUserId(@PathVariable String userId) {
        log.info("[API] 查询用户敏感操作日志: userId={}", userId);
        return sensitiveOperationLogService.listByUserId(userId);
    }

    @GetMapping("/risk-level/{riskScore}")
    public List<SysSensitiveOperationLog> listByRiskLevel(@PathVariable Integer riskScore) {
        log.info("[API] 查询风险等级敏感操作日志: riskScore={}", riskScore);
        return sensitiveOperationLogService.listByRiskLevel(riskScore);
    }

    @GetMapping("/page")
    public Page<SysSensitiveOperationLog> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Integer riskScore) {
        log.info("[API] 分页查询敏感操作日志: page={}, size={}", page, size);
        return sensitiveOperationLogService.pageQuery(page, size, userId, operationType, riskScore);
    }
}
