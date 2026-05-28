package scm.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.supplier.domain.entity.SupSupplierEvaluation;
import scm.supplier.service.ISupSupplierEvaluationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/supplier-evaluations")
@Tag(name = "供应商评价管理", description = "供应商评价CRUD及查询接口")
public class SupSupplierEvaluationController {

    @Autowired
    private ISupSupplierEvaluationService evaluationService;

    @GetMapping("/{id}")
    @Operation(summary = "查询评价详情")
    public ApiResponse<SupSupplierEvaluation> getById(@PathVariable String id) {
        log.info("[API] 查询评价详情: id={}", id);
        SupSupplierEvaluation evaluation = evaluationService.getById(id);
        if (evaluation == null) {
            return ApiResponse.fail(404, "评价不存在");
        }
        return ApiResponse.success(evaluation);
    }

    @PostMapping
    @Operation(summary = "创建评价")
    public ApiResponse<SupSupplierEvaluation> create(@RequestBody SupSupplierEvaluation evaluation) {
        log.info("[API] 创建供应商评价: supplierId={}", evaluation.getSupplierId());
        evaluation.setId(UUID.randomUUID().toString());
        if (evaluation.getEvaluatedAt() == null) {
            evaluation.setEvaluatedAt(LocalDateTime.now());
        }
        evaluation.setCreateTime(LocalDateTime.now());
        evaluation.setUpdateTime(LocalDateTime.now());

        if (evaluation.getTotalScore() == null) {
            evaluation.setTotalScore(calculateTotalScore(evaluation));
        }

        evaluationService.save(evaluation);
        log.info("[API] 评价创建成功: id={}", evaluation.getId());
        return ApiResponse.success(evaluation);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新评价")
    public ApiResponse<SupSupplierEvaluation> update(@PathVariable String id,
                                                     @RequestBody SupSupplierEvaluation evaluation) {
        log.info("[API] 更新评价: id={}", id);
        SupSupplierEvaluation existing = evaluationService.getById(id);
        if (existing == null) {
            return ApiResponse.fail(404, "评价不存在");
        }
        evaluation.setId(id);
        evaluation.setUpdateTime(LocalDateTime.now());
        if (evaluation.getTotalScore() == null) {
            evaluation.setTotalScore(calculateTotalScore(evaluation));
        }
        evaluationService.updateById(evaluation);
        return ApiResponse.success(evaluationService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评价")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除评价: id={}", id);
        boolean success = evaluationService.removeById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "删除失败");
    }

    @GetMapping
    @Operation(summary = "分页查询评价列表")
    public ApiResponse<Page<SupSupplierEvaluation>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String evaluationPeriod) {
        log.info("[API] 分页查询评价: page={}, size={}, supplierId={}", page, size, supplierId);
        Page<SupSupplierEvaluation> result = evaluationService.pageList(page, size, supplierId, evaluationPeriod);
        return ApiResponse.success(result);
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "查询供应商的所有评价")
    public ApiResponse<List<SupSupplierEvaluation>> listBySupplierId(@PathVariable String supplierId) {
        log.info("[API] 查询供应商评价列表: supplierId={}", supplierId);
        return ApiResponse.success(evaluationService.listBySupplierId(supplierId));
    }

    @GetMapping("/supplier/{supplierId}/average-score")
    @Operation(summary = "计算供应商平均评分")
    public ApiResponse<BigDecimal> getAverageScore(@PathVariable String supplierId) {
        log.info("[API] 计算供应商平均评分: supplierId={}", supplierId);
        return ApiResponse.success(evaluationService.calculateAverageScore(supplierId));
    }

    private BigDecimal calculateTotalScore(SupSupplierEvaluation evaluation) {
        BigDecimal quality = evaluation.getQualityScore() != null ? evaluation.getQualityScore() : BigDecimal.ZERO;
        BigDecimal delivery = evaluation.getDeliveryScore() != null ? evaluation.getDeliveryScore() : BigDecimal.ZERO;
        BigDecimal service = evaluation.getServiceScore() != null ? evaluation.getServiceScore() : BigDecimal.ZERO;
        BigDecimal price = evaluation.getPriceScore() != null ? evaluation.getPriceScore() : BigDecimal.ZERO;
        return quality.add(delivery).add(service).add(price);
    }
}
