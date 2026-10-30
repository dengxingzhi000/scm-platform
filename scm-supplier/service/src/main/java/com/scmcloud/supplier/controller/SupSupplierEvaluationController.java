package com.scmcloud.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.supplier.domain.entity.SupSupplierEvaluation;
import com.scmcloud.supplier.service.ISupSupplierEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/supplier-evaluations")
@Tag(name = "供应商评估", description = "提供供应商评价的创建、查询、得分统计与删除接口。")
public class SupSupplierEvaluationController {

    private final ISupSupplierEvaluationService evaluationService;

    @Operation(summary = "查询评价详情", description = "根据评价ID查询供应商评价详情。")
    @GetMapping("/{id}")
    public ApiResponse<SupSupplierEvaluation> getById(@Parameter(description = "评价ID") @PathVariable String id) {
        log.info("[API] 查询评价详情: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        SupSupplierEvaluation evaluation = evaluationService.getById(id);
        if (evaluation == null) {
            return ApiResponse.fail(404, "评价不存在");
        }
        return ApiResponse.success(evaluation);
    }

    @Operation(summary = "创建供应商评价", description = "新增供应商评价，按质量/交付/服务/价格加权计算总分并回写供应商绩效。")
    @PostMapping
    public ApiResponse<SupSupplierEvaluation> create(@RequestBody SupSupplierEvaluation evaluation) {
        log.info("[API] 创建供应商评价 supplierId={}", evaluation.getSupplierId());
        return ApiResponse.success(evaluationService.createEvaluation(evaluation));
    }

    @Operation(summary = "更新评价", description = "根据评价ID更新供应商评价并重新计算总分、回写供应商绩效。")
    @PutMapping("/{id}")
    public ApiResponse<SupSupplierEvaluation> update(@Parameter(description = "评价ID") @PathVariable String id,
                                                     @RequestBody SupSupplierEvaluation evaluation) {
        log.info("[API] 更新评价: id={}", id);
        return ApiResponse.success(evaluationService.updateEvaluation(id, evaluation));
    }

    @Operation(summary = "删除评价", description = "根据评价ID删除供应商评价并重新回写供应商绩效。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "评价ID") @PathVariable String id) {
        log.info("[API] 删除评价: id={}", id);
        boolean success = evaluationService.deleteEvaluation(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "评价不存在");
    }

    @Operation(summary = "分页查询评价", description = "按供应商与评价周期分页查询供应商评价列表。")
    @GetMapping
    public ApiResponse<Page<SupSupplierEvaluation>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "供应商ID") @RequestParam(required = false) String supplierId,
            @Parameter(description = "评价周期，例如 2026-09") @RequestParam(required = false) String evaluationPeriod) {
        log.info("[API] 分页查询评价: page={}, size={}, supplierId={}", (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (supplierId + "").replace("\r", "_").replace("\n", "_"));
        Page<SupSupplierEvaluation> result = evaluationService.pageList(page, size, supplierId, evaluationPeriod);
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询供应商评价列表", description = "查询指定供应商的全部评价记录。")
    @GetMapping("/supplier/{supplierId}")
    public ApiResponse<List<SupSupplierEvaluation>> listBySupplierId(@Parameter(description = "供应商ID") @PathVariable String supplierId) {
        log.info("[API] 查询供应商评价列表 supplierId={}", (supplierId + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(evaluationService.listBySupplierId(supplierId));
    }

    @Operation(summary = "查询供应商平均得分", description = "计算并返回指定供应商的平均评价得分。")
    @GetMapping("/supplier/{supplierId}/average-score")
    public ApiResponse<BigDecimal> getAverageScore(@Parameter(description = "供应商ID") @PathVariable String supplierId) {
        log.info("[API] 计算供应商平均评价 supplierId={}", (supplierId + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(evaluationService.calculateAverageScore(supplierId));
    }

}
