package scm.purchase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurPlanItem;
import scm.purchase.service.IPurPlanItemService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-plan-item")
@Tag(name = "采购计划明细管理", description = "采购计划明细CRUD接口")
public class PurPlanItemController {

    @Autowired
    private IPurPlanItemService purPlanItemService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询明细")
    public PurPlanItem getById(@PathVariable String id) {
        return purPlanItemService.getById(id);
    }

    @GetMapping("/list/{planId}")
    @Operation(summary = "根据计划ID查询明细列表")
    public List<PurPlanItem> listByPlanId(@PathVariable String planId) {
        return purPlanItemService.listByPlanId(planId);
    }

    @PostMapping
    @Operation(summary = "创建明细")
    public boolean save(@RequestBody PurPlanItem purPlanItem) {
        return purPlanItemService.save(purPlanItem);
    }

    @PutMapping
    @Operation(summary = "更新明细")
    public boolean update(@RequestBody PurPlanItem purPlanItem) {
        return purPlanItemService.updateById(purPlanItem);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除明细")
    public boolean delete(@PathVariable String id) {
        return purPlanItemService.removeById(id);
    }

    @DeleteMapping("/plan/{planId}")
    @Operation(summary = "根据计划ID删除所有明细")
    public boolean deleteByPlanId(@PathVariable String planId) {
        return purPlanItemService.deleteByPlanId(planId);
    }
}
