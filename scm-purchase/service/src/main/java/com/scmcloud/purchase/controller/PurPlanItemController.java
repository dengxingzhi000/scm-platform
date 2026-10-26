package com.scmcloud.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurPlanItem;
import com.scmcloud.purchase.service.IPurPlanItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-plan-item")
@Tag(name = "采购计划明细", description = "采购计划明细行的查询与维护。")
public class PurPlanItemController {

    private final IPurPlanItemService purPlanItemService;

    @Operation(summary = "根据 ID 查询计划明细", description = "按主键查询单条采购计划明细。")
    @GetMapping("/{id}")
    public PurPlanItem getById(@Parameter(description = "采购计划明细 ID") @PathVariable String id) {
        return purPlanItemService.getById(id);
    }

    @Operation(summary = "按计划查询明细列表", description = "查询指定采购计划下的全部明细行。")
    @GetMapping("/list/{planId}")
    public List<PurPlanItem> listByPlanId(@Parameter(description = "采购计划 ID") @PathVariable String planId) {
        return purPlanItemService.listByPlanId(planId);
    }

    @Operation(summary = "新增计划明细", description = "为采购计划新增一条明细行。")
    @PostMapping
    public boolean save(@RequestBody PurPlanItem purPlanItem) {
        return purPlanItemService.save(purPlanItem);
    }

    @Operation(summary = "更新计划明细", description = "按主键更新采购计划明细信息。")
    @PutMapping
    public boolean update(@RequestBody PurPlanItem purPlanItem) {
        return purPlanItemService.updateById(purPlanItem);
    }

    @Operation(summary = "删除计划明细", description = "按主键删除单条采购计划明细。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "采购计划明细 ID") @PathVariable String id) {
        return purPlanItemService.removeById(id);
    }

    @Operation(summary = "按计划删除明细", description = "删除指定采购计划下的全部明细行。")
    @DeleteMapping("/plan/{planId}")
    public boolean deleteByPlanId(@Parameter(description = "采购计划 ID") @PathVariable String planId) {
        return purPlanItemService.deleteByPlanId(planId);
    }
}
