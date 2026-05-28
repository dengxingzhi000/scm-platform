package scm.purchase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurPriceComparisonItem;
import scm.purchase.service.IPurPriceComparisonItemService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-price-comparison-item")
@Tag(name = "比价明细管理", description = "比价明细CRUD接口")
public class PurPriceComparisonItemController {

    @Autowired
    private IPurPriceComparisonItemService purPriceComparisonItemService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询明细")
    public PurPriceComparisonItem getById(@PathVariable String id) {
        return purPriceComparisonItemService.getById(id);
    }

    @GetMapping("/list/{comparisonId}")
    @Operation(summary = "根据比价ID查询明细列表")
    public List<PurPriceComparisonItem> listByComparisonId(@PathVariable String comparisonId) {
        return purPriceComparisonItemService.listByComparisonId(comparisonId);
    }

    @PostMapping
    @Operation(summary = "创建明细")
    public boolean save(@RequestBody PurPriceComparisonItem purPriceComparisonItem) {
        return purPriceComparisonItemService.save(purPriceComparisonItem);
    }

    @PutMapping
    @Operation(summary = "更新明细")
    public boolean update(@RequestBody PurPriceComparisonItem purPriceComparisonItem) {
        return purPriceComparisonItemService.updateById(purPriceComparisonItem);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除明细")
    public boolean delete(@PathVariable String id) {
        return purPriceComparisonItemService.removeById(id);
    }

    @DeleteMapping("/comparison/{comparisonId}")
    @Operation(summary = "根据比价ID删除所有明细")
    public boolean deleteByComparisonId(@PathVariable String comparisonId) {
        return purPriceComparisonItemService.deleteByComparisonId(comparisonId);
    }
}
