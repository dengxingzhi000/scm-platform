package scm.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurPlanItem;
import scm.purchase.service.IPurPlanItemService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-plan-item")
public class PurPlanItemController {

    private final IPurPlanItemService purPlanItemService;

    @GetMapping("/{id}")
    public PurPlanItem getById(@PathVariable String id) {
        return purPlanItemService.getById(id);
    }

    @GetMapping("/list/{planId}")
    public List<PurPlanItem> listByPlanId(@PathVariable String planId) {
        return purPlanItemService.listByPlanId(planId);
    }

    @PostMapping
    public boolean save(@RequestBody PurPlanItem purPlanItem) {
        return purPlanItemService.save(purPlanItem);
    }

    @PutMapping
    public boolean update(@RequestBody PurPlanItem purPlanItem) {
        return purPlanItemService.updateById(purPlanItem);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purPlanItemService.removeById(id);
    }

    @DeleteMapping("/plan/{planId}")
    public boolean deleteByPlanId(@PathVariable String planId) {
        return purPlanItemService.deleteByPlanId(planId);
    }
}
