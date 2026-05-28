package scm.purchase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurQuotationItem;
import scm.purchase.service.IPurQuotationItemService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-quotation-item")
@Tag(name = "报价明细管理", description = "报价明细CRUD接口")
public class PurQuotationItemController {

    @Autowired
    private IPurQuotationItemService purQuotationItemService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询明细")
    public PurQuotationItem getById(@PathVariable String id) {
        return purQuotationItemService.getById(id);
    }

    @GetMapping("/list/{quotationId}")
    @Operation(summary = "根据报价单ID查询明细列表")
    public List<PurQuotationItem> listByQuotationId(@PathVariable String quotationId) {
        return purQuotationItemService.listByQuotationId(quotationId);
    }

    @PostMapping
    @Operation(summary = "创建明细")
    public boolean save(@RequestBody PurQuotationItem purQuotationItem) {
        return purQuotationItemService.save(purQuotationItem);
    }

    @PutMapping
    @Operation(summary = "更新明细")
    public boolean update(@RequestBody PurQuotationItem purQuotationItem) {
        return purQuotationItemService.updateById(purQuotationItem);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除明细")
    public boolean delete(@PathVariable String id) {
        return purQuotationItemService.removeById(id);
    }

    @DeleteMapping("/quotation/{quotationId}")
    @Operation(summary = "根据报价单ID删除所有明细")
    public boolean deleteByQuotationId(@PathVariable String quotationId) {
        return purQuotationItemService.deleteByQuotationId(quotationId);
    }
}
