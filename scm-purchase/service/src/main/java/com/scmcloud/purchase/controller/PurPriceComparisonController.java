package com.scmcloud.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurPriceComparison;
import com.scmcloud.purchase.service.IPurPriceComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-price-comparison")
@Tag(name = "比价管理", description = "供应商比价单的查询、维护与审批管理。")
public class PurPriceComparisonController {

    private final IPurPriceComparisonService purPriceComparisonService;

    @Operation(summary = "根据 ID 查询比价单", description = "按主键查询比价单的详细信息。")
    @GetMapping("/{id}")
    public PurPriceComparison getById(@Parameter(description = "比价单 ID") @PathVariable String id) {
        return purPriceComparisonService.getById(id);
    }

    @Operation(summary = "根据比价单号查询", description = "通过比价单号查询比价单详情。")
    @GetMapping("/no/{comparisonNo}")
    public PurPriceComparison getByComparisonNo(@Parameter(description = "比价单号") @PathVariable String comparisonNo) {
        return purPriceComparisonService.getByComparisonNo(comparisonNo);
    }

    @Operation(summary = "分页查询比价单", description = "按状态和关联询价单分页查询比价单。")
    @GetMapping("/page")
    public Page<PurPriceComparison> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @Parameter(description = "关联的询价单 ID") @RequestParam(required = false) String rfqId) {
        return purPriceComparisonService.pageQuery(page, size, status, rfqId);
    }

    @Operation(summary = "按询价单查询比价单列表", description = "查询指定询价单关联的全部比价单。")
    @GetMapping("/rfq/{rfqId}")
    public List<PurPriceComparison> listByRfqId(@Parameter(description = "询价单 ID") @PathVariable String rfqId) {
        return purPriceComparisonService.listByRfqId(rfqId);
    }

    @Operation(summary = "新增比价单", description = "创建一份新的供应商比价单。")
    @PostMapping
    public boolean save(@RequestBody PurPriceComparison purPriceComparison) {
        return purPriceComparisonService.save(purPriceComparison);
    }

    @Operation(summary = "更新比价单", description = "按主键更新比价单信息。")
    @PutMapping
    public boolean update(@RequestBody PurPriceComparison purPriceComparison) {
        return purPriceComparisonService.updateById(purPriceComparison);
    }

    @Operation(summary = "删除比价单", description = "按主键删除比价单。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "比价单 ID") @PathVariable String id) {
        return purPriceComparisonService.removeById(id);
    }

    @Operation(summary = "审批比价单", description = "记录审批人信息并通过比价单审批。")
    @PostMapping("/{id}/approve")
    public boolean approve(
            @Parameter(description = "比价单 ID") @PathVariable String id,
            @Parameter(description = "审批人 ID") @RequestParam String approverId,
            @Parameter(description = "审批人姓名") @RequestParam String approverName) {
        return purPriceComparisonService.approve(id, approverId, approverName);
    }
}
