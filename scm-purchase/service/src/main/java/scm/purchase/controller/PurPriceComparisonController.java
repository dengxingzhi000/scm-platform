package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurPriceComparison;
import scm.purchase.service.IPurPriceComparisonService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-price-comparison")
@Tag(name = "比价分析管理", description = "比价分析CRUD及审批接口")
public class PurPriceComparisonController {

    @Autowired
    private IPurPriceComparisonService purPriceComparisonService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询比价分析")
    public PurPriceComparison getById(@PathVariable String id) {
        return purPriceComparisonService.getById(id);
    }

    @GetMapping("/no/{comparisonNo}")
    @Operation(summary = "根据比价编号查询")
    public PurPriceComparison getByComparisonNo(@PathVariable String comparisonNo) {
        return purPriceComparisonService.getByComparisonNo(comparisonNo);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询比价分析")
    public Page<PurPriceComparison> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String rfqId) {
        return purPriceComparisonService.pageQuery(page, size, status, rfqId);
    }

    @GetMapping("/rfq/{rfqId}")
    @Operation(summary = "根据询价单ID查询比价列表")
    public List<PurPriceComparison> listByRfqId(@PathVariable String rfqId) {
        return purPriceComparisonService.listByRfqId(rfqId);
    }

    @PostMapping
    @Operation(summary = "创建比价分析")
    public boolean save(@RequestBody PurPriceComparison purPriceComparison) {
        return purPriceComparisonService.save(purPriceComparison);
    }

    @PutMapping
    @Operation(summary = "更新比价分析")
    public boolean update(@RequestBody PurPriceComparison purPriceComparison) {
        return purPriceComparisonService.updateById(purPriceComparison);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除比价分析")
    public boolean delete(@PathVariable String id) {
        return purPriceComparisonService.removeById(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批比价分析")
    public boolean approve(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName) {
        return purPriceComparisonService.approve(id, approverId, approverName);
    }
}
