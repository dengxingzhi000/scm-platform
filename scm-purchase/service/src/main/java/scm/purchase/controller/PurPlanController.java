package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurPlan;
import scm.purchase.service.IPurPlanService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-plan")
@Tag(name = "采购计划管理", description = "采购计划CRUD及工作流接口")
public class PurPlanController {

    @Autowired
    private IPurPlanService purPlanService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询采购计划")
    public PurPlan getById(@PathVariable String id) {
        return purPlanService.getById(id);
    }

    @GetMapping("/no/{planNo}")
    @Operation(summary = "根据计划编号查询")
    public PurPlan getByPlanNo(@PathVariable String planNo) {
        return purPlanService.getByPlanNo(planNo);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询采购计划")
    public Page<PurPlan> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer planType,
            @RequestParam(required = false) String keyword) {
        return purPlanService.pageQuery(page, size, status, planType, keyword);
    }

    @GetMapping("/list")
    @Operation(summary = "根据状态查询列表")
    public List<PurPlan> listByStatus(@RequestParam Integer status) {
        return purPlanService.listByStatus(status);
    }

    @PostMapping
    @Operation(summary = "创建采购计划")
    public boolean save(@RequestBody PurPlan purPlan) {
        return purPlanService.save(purPlan);
    }

    @PutMapping
    @Operation(summary = "更新采购计划")
    public boolean update(@RequestBody PurPlan purPlan) {
        return purPlanService.updateById(purPlan);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除采购计划")
    public boolean delete(@PathVariable String id) {
        return purPlanService.removeById(id);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交采购计划")
    public boolean submit(@PathVariable String id) {
        return purPlanService.submit(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批采购计划")
    public boolean approve(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName) {
        return purPlanService.approve(id, approverId, approverName);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成采购计划")
    public boolean complete(@PathVariable String id) {
        return purPlanService.complete(id);
    }
}
