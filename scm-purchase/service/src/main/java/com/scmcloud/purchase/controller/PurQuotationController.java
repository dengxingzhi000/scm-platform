package com.scmcloud.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurQuotation;
import com.scmcloud.purchase.service.IPurQuotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-quotation")
@Tag(name = "供应商报价", description = "供应商报价单的查询、维护、提交与选用管理。")
public class PurQuotationController {

    private final IPurQuotationService purQuotationService;

    @Operation(summary = "根据 ID 查询报价单", description = "按主键查询供应商报价单的详细信息。")
    @GetMapping("/{id}")
    public PurQuotation getById(@Parameter(description = "报价单 ID") @PathVariable String id) {
        return purQuotationService.getById(id);
    }

    @Operation(summary = "根据报价单号查询", description = "通过报价单号查询供应商报价单详情。")
    @GetMapping("/no/{quotationNo}")
    public PurQuotation getByQuotationNo(@Parameter(description = "报价单号") @PathVariable String quotationNo) {
        return purQuotationService.getByQuotationNo(quotationNo);
    }

    @Operation(summary = "分页查询报价单", description = "按状态、供应商和关联询价单分页查询报价单。")
    @GetMapping("/page")
    public Page<PurQuotation> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @Parameter(description = "供应商 ID") @RequestParam(required = false) String supplierId,
            @Parameter(description = "关联的询价单 ID") @RequestParam(required = false) String rfqId) {
        return purQuotationService.pageQuery(page, size, status, supplierId, rfqId);
    }

    @Operation(summary = "按询价单查询报价列表", description = "查询指定询价单收到的全部供应商报价。")
    @GetMapping("/rfq/{rfqId}")
    public List<PurQuotation> listByRfqId(@Parameter(description = "询价单 ID") @PathVariable String rfqId) {
        return purQuotationService.listByRfqId(rfqId);
    }

    @Operation(summary = "按供应商查询报价列表", description = "查询指定供应商提交的全部报价单。")
    @GetMapping("/supplier/{supplierId}")
    public List<PurQuotation> listBySupplierId(@Parameter(description = "供应商 ID") @PathVariable String supplierId) {
        return purQuotationService.listBySupplierId(supplierId);
    }

    @Operation(summary = "新增报价单", description = "创建一份新的供应商报价单。")
    @PostMapping
    public boolean save(@RequestBody PurQuotation purQuotation) {
        return purQuotationService.save(purQuotation);
    }

    @Operation(summary = "更新报价单", description = "按主键更新供应商报价单信息。")
    @PutMapping
    public boolean update(@RequestBody PurQuotation purQuotation) {
        return purQuotationService.updateById(purQuotation);
    }

    @Operation(summary = "删除报价单", description = "按主键删除供应商报价单。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "报价单 ID") @PathVariable String id) {
        return purQuotationService.removeById(id);
    }

    @Operation(summary = "提交报价单", description = "将供应商报价单提交，推进到待评审状态。")
    @PostMapping("/{id}/submit")
    public boolean submit(@Parameter(description = "报价单 ID") @PathVariable String id) {
        return purQuotationService.submit(id);
    }

    @Operation(summary = "选用报价单", description = "记录选用人信息并将该报价单标记为已选用。")
    @PostMapping("/{id}/select")
    public boolean select(
            @Parameter(description = "报价单 ID") @PathVariable String id,
            @Parameter(description = "选用人 ID") @RequestParam String selectedBy,
            @Parameter(description = "选用人姓名") @RequestParam String selectedByName) {
        return purQuotationService.select(id, selectedBy, selectedByName);
    }
}
