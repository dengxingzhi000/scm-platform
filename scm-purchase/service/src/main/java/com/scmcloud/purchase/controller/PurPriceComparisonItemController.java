package com.scmcloud.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurPriceComparisonItem;
import com.scmcloud.purchase.service.IPurPriceComparisonItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-price-comparison-item")
@Tag(name = "比价明细", description = "比价单中各家供应商报价明细的查询与维护。")
public class PurPriceComparisonItemController {

    private final IPurPriceComparisonItemService purPriceComparisonItemService;

    @Operation(summary = "根据 ID 查询比价明细", description = "按主键查询单条比价明细。")
    @GetMapping("/{id}")
    public PurPriceComparisonItem getById(@Parameter(description = "比价明细 ID") @PathVariable String id) {
        return purPriceComparisonItemService.getById(id);
    }

    @Operation(summary = "按比价单查询明细列表", description = "查询指定比价单下的全部比价明细。")
    @GetMapping("/list/{comparisonId}")
    public List<PurPriceComparisonItem> listByComparisonId(@Parameter(description = "比价单 ID") @PathVariable String comparisonId) {
        return purPriceComparisonItemService.listByComparisonId(comparisonId);
    }

    @Operation(summary = "新增比价明细", description = "为比价单新增一条供应商报价明细。")
    @PostMapping
    public boolean save(@RequestBody PurPriceComparisonItem purPriceComparisonItem) {
        return purPriceComparisonItemService.save(purPriceComparisonItem);
    }

    @Operation(summary = "更新比价明细", description = "按主键更新比价明细信息。")
    @PutMapping
    public boolean update(@RequestBody PurPriceComparisonItem purPriceComparisonItem) {
        return purPriceComparisonItemService.updateById(purPriceComparisonItem);
    }

    @Operation(summary = "删除比价明细", description = "按主键删除单条比价明细。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "比价明细 ID") @PathVariable String id) {
        return purPriceComparisonItemService.removeById(id);
    }

    @Operation(summary = "按比价单删除明细", description = "删除指定比价单下的全部比价明细。")
    @DeleteMapping("/comparison/{comparisonId}")
    public boolean deleteByComparisonId(@Parameter(description = "比价单 ID") @PathVariable String comparisonId) {
        return purPriceComparisonItemService.deleteByComparisonId(comparisonId);
    }
}
