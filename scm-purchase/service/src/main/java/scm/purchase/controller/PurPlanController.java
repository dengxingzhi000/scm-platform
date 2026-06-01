package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurPlan;
import scm.purchase.service.IPurPlanService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-plan")
public class PurPlanController {

    private final IPurPlanService purPlanService;

    @GetMapping("/{id}")
    public PurPlan getById(@PathVariable String id) {
        return purPlanService.getById(id);
    }

    @GetMapping("/no/{planNo}")
    public PurPlan getByPlanNo(@PathVariable String planNo) {
        return purPlanService.getByPlanNo(planNo);
    }

    @GetMapping("/page")
    public Page<PurPlan> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer planType,
            @RequestParam(required = false) String keyword) {
        return purPlanService.pageQuery(page, size, status, planType, keyword);
    }

    @GetMapping("/list")
    public List<PurPlan> listByStatus(@RequestParam Integer status) {
        return purPlanService.listByStatus(status);
    }

    @PostMapping
    public boolean save(@RequestBody PurPlan purPlan) {
        return purPlanService.save(purPlan);
    }

    @PutMapping
    public boolean update(@RequestBody PurPlan purPlan) {
        return purPlanService.updateById(purPlan);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purPlanService.removeById(id);
    }

    @PostMapping("/{id}/submit")
    public boolean submit(@PathVariable String id) {
        return purPlanService.submit(id);
    }

    @PostMapping("/{id}/approve")
    public boolean approve(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName) {
        return purPlanService.approve(id, approverId, approverName);
    }

    @PostMapping("/{id}/complete")
    public boolean complete(@PathVariable String id) {
        return purPlanService.complete(id);
    }
}
