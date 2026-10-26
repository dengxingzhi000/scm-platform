package com.scmcloud.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurQuotationItem;
import com.scmcloud.purchase.service.IPurQuotationItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-quotation-item")
@Tag(name = "报价明细", description = "供应商报价单明细行的查询与维护接口")
public class PurQuotationItemController {

    private final IPurQuotationItemService purQuotationItemService;

    @Operation(summary = "根据 ID 查询报价明细", description = "按明细主键查询单条供应商报价明细记录。")
    @GetMapping("/{id}")
    public PurQuotationItem getById(@Parameter(description = "报价明细主键 ID") @PathVariable String id) {
        return purQuotationItemService.getById(id);
    }

    @Operation(summary = "查询报价单下的明细列表", description = "根据报价单 ID 查询该报价单包含的全部明细行。")
    @GetMapping("/list/{quotationId}")
    public List<PurQuotationItem> listByQuotationId(@Parameter(description = "报价单 ID") @PathVariable String quotationId) {
        return purQuotationItemService.listByQuotationId(quotationId);
    }

    @Operation(summary = "新增报价明细", description = "创建一条新的供应商报价明细记录。")
    @PostMapping
    public boolean save(@RequestBody PurQuotationItem purQuotationItem) {
        return purQuotationItemService.save(purQuotationItem);
    }

    @Operation(summary = "修改报价明细", description = "按主键更新已有的报价明细记录。")
    @PutMapping
    public boolean update(@RequestBody PurQuotationItem purQuotationItem) {
        return purQuotationItemService.updateById(purQuotationItem);
    }

    @Operation(summary = "删除报价明细", description = "按明细主键删除单条报价明细记录。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "报价明细主键 ID") @PathVariable String id) {
        return purQuotationItemService.removeById(id);
    }

    @Operation(summary = "按报价单删除明细", description = "删除指定报价单下的全部明细行。")
    @DeleteMapping("/quotation/{quotationId}")
    public boolean deleteByQuotationId(@Parameter(description = "报价单 ID") @PathVariable String quotationId) {
        return purQuotationItemService.deleteByQuotationId(quotationId);
    }
}
