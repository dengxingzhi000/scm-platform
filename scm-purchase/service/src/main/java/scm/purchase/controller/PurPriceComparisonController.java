package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurPriceComparison;
import scm.purchase.service.IPurPriceComparisonService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-price-comparison")
public class PurPriceComparisonController {

    private final IPurPriceComparisonService purPriceComparisonService;

    @GetMapping("/{id}")
    public PurPriceComparison getById(@PathVariable String id) {
        return purPriceComparisonService.getById(id);
    }

    @GetMapping("/no/{comparisonNo}")
    public PurPriceComparison getByComparisonNo(@PathVariable String comparisonNo) {
        return purPriceComparisonService.getByComparisonNo(comparisonNo);
    }

    @GetMapping("/page")
    public Page<PurPriceComparison> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String rfqId) {
        return purPriceComparisonService.pageQuery(page, size, status, rfqId);
    }

    @GetMapping("/rfq/{rfqId}")
    public List<PurPriceComparison> listByRfqId(@PathVariable String rfqId) {
        return purPriceComparisonService.listByRfqId(rfqId);
    }

    @PostMapping
    public boolean save(@RequestBody PurPriceComparison purPriceComparison) {
        return purPriceComparisonService.save(purPriceComparison);
    }

    @PutMapping
    public boolean update(@RequestBody PurPriceComparison purPriceComparison) {
        return purPriceComparisonService.updateById(purPriceComparison);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purPriceComparisonService.removeById(id);
    }

    @PostMapping("/{id}/approve")
    public boolean approve(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName) {
        return purPriceComparisonService.approve(id, approverId, approverName);
    }
}
