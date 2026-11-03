package com.scmcloud.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurPlan;
import com.scmcloud.purchase.service.IPurPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-plan")
@Tag(name = "采购计划", description = "采购计划的查询、维护、提交、审批与完结管理。")
public class PurPlanController {

    private final IPurPlanService purPlanService;

    @Operation(summary = "根据 ID 查询计划", description = "按主键查询采购计划的详细信息。")
    @GetMapping("/{id}")
    public PurPlan getById(@Parameter(description = "采购计划 ID") @PathVariable String id) {
        return purPlanService.getById(id);
    }

    @Operation(summary = "根据计划编号查询", description = "通过计划编号查询采购计划详情。")
    @GetMapping("/no/{planNo}")
    public PurPlan getByPlanNo(@Parameter(description = "采购计划编号") @PathVariable String planNo) {
        return purPlanService.getByPlanNo(planNo);
    }

    @Operation(summary = "分页查询计划", description = "按状态、计划类型和关键字分页查询采购计划。")
    @GetMapping("/page")
    public Page<PurPlan> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @Parameter(description = "计划类型") @RequestParam(required = false) Integer planType,
            @RequestParam(required = false) String keyword) {
        return purPlanService.pageQuery(page, size, status, planType, keyword);
    }

    @Operation(summary = "按状态查询计划列表", description = "查询指定状态下的全部采购计划。")
    @GetMapping("/list")
    public List<PurPlan> listByStatus(@Parameter(description = "计划状态") @RequestParam Integer status) {
        return purPlanService.listByStatus(status);
    }

    @Operation(summary = "新增计划", description = "创建一份新的采购计划。")
    @PostMapping
    public boolean save(@RequestBody PurPlan purPlan) {
        return purPlanService.save(purPlan);
    }

    @Operation(summary = "更新计划", description = "按主键更新采购计划信息。")
    @PutMapping
    public boolean update(@RequestBody PurPlan purPlan) {
        return purPlanService.updateById(purPlan);
    }

    @Operation(summary = "删除计划", description = "按主键删除采购计划。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "采购计划 ID") @PathVariable String id) {
        return purPlanService.removeById(id);
    }

    @Operation(summary = "提交计划", description = "将采购计划提交审批，推进到待审批状态。")
    @PostMapping("/{id}/submit")
    public boolean submit(@Parameter(description = "采购计划 ID") @PathVariable String id) {
        return purPlanService.submit(id);
    }

    @Operation(summary = "审批计划", description = "记录审批人信息并通过采购计划审批。")
    @PostMapping("/{id}/approve")
    public boolean approve(
            @Parameter(description = "采购计划 ID") @PathVariable String id,
            @Parameter(description = "审批人 ID") @RequestParam String approverId,
            @Parameter(description = "审批人姓名") @RequestParam String approverName) {
        return purPlanService.approve(id, approverId, approverName);
    }

    @Operation(summary = "完结计划", description = "将采购计划标记为已完成并结束执行。")
    @PostMapping("/{id}/complete")
    public boolean complete(@Parameter(description = "采购计划 ID") @PathVariable String id) {
        return purPlanService.complete(id);
    }
}
