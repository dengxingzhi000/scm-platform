package scm.audit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.audit.domain.entity.SysSensitiveOperationLog;
import scm.audit.service.impl.SysSensitiveOperationLogServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sys-sensitive-operation-log")
@Tag(name = "敏感操作日志管理", description = "敏感操作日志的增删改查接口")
public class SysSensitiveOperationLogController {

    @Autowired
    private SysSensitiveOperationLogServiceImpl sensitiveOperationLogService;

    @PostMapping
    @Operation(summary = "创建敏感操作日志")
    public SysSensitiveOperationLog create(@RequestBody SysSensitiveOperationLog entity) {
        log.info("[API] 创建敏感操作日志: userId={}, operationType={}", entity.getUserId(), entity.getOperationType());
        return sensitiveOperationLogService.createLog(entity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询敏感操作日志")
    public SysSensitiveOperationLog getById(@PathVariable String id) {
        log.info("[API] 查询敏感操作日志: id={}", id);
        return sensitiveOperationLogService.getById(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除敏感操作日志")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除敏感操作日志: id={}", id);
        return sensitiveOperationLogService.deleteById(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询敏感操作日志")
    public List<SysSensitiveOperationLog> listByUserId(@PathVariable String userId) {
        log.info("[API] 查询用户敏感操作日志: userId={}", userId);
        return sensitiveOperationLogService.listByUserId(userId);
    }

    @GetMapping("/risk-level/{riskScore}")
    @Operation(summary = "根据风险等级查询敏感操作日志")
    public List<SysSensitiveOperationLog> listByRiskLevel(@PathVariable Integer riskScore) {
        log.info("[API] 查询风险等级敏感操作日志: riskScore={}", riskScore);
        return sensitiveOperationLogService.listByRiskLevel(riskScore);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询敏感操作日志")
    public Page<SysSensitiveOperationLog> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "风险评分") @RequestParam(required = false) Integer riskScore) {
        log.info("[API] 分页查询敏感操作日志: page={}, size={}", page, size);
        return sensitiveOperationLogService.pageQuery(page, size, userId, operationType, riskScore);
    }
}
