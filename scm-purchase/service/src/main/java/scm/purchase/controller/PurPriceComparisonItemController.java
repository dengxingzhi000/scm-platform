package scm.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurPriceComparisonItem;
import scm.purchase.service.IPurPriceComparisonItemService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-price-comparison-item")
public class PurPriceComparisonItemController {

    private final IPurPriceComparisonItemService purPriceComparisonItemService;

    @GetMapping("/{id}")
    public PurPriceComparisonItem getById(@PathVariable String id) {
        return purPriceComparisonItemService.getById(id);
    }

    @GetMapping("/list/{comparisonId}")
    public List<PurPriceComparisonItem> listByComparisonId(@PathVariable String comparisonId) {
        return purPriceComparisonItemService.listByComparisonId(comparisonId);
    }

    @PostMapping
    public boolean save(@RequestBody PurPriceComparisonItem purPriceComparisonItem) {
        return purPriceComparisonItemService.save(purPriceComparisonItem);
    }

    @PutMapping
    public boolean update(@RequestBody PurPriceComparisonItem purPriceComparisonItem) {
        return purPriceComparisonItemService.updateById(purPriceComparisonItem);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purPriceComparisonItemService.removeById(id);
    }

    @DeleteMapping("/comparison/{comparisonId}")
    public boolean deleteByComparisonId(@PathVariable String comparisonId) {
        return purPriceComparisonItemService.deleteByComparisonId(comparisonId);
    }
}
