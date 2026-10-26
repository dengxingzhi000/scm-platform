package com.scmcloud.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.finance.domain.dto.CreateInvoiceRequest;
import com.scmcloud.finance.domain.dto.UpdateInvoiceRequest;
import com.scmcloud.finance.domain.entity.Invoice;
import com.scmcloud.finance.service.IInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/invoice")
@Tag(name = "发票管理", description = "管理发票的创建、开具、作废与红冲等全生命周期操作。")
public class InvoiceController {
    private final IInvoiceService invoiceService;

    @Operation(summary = "查询发票详情", description = "根据发票 ID 查询发票的完整开票信息。")
    @GetMapping("/{id}")
    public ApiResponse<Invoice> getById(@Parameter(description = "发票 ID") @PathVariable String id) {
        Invoice invoice = invoiceService.getById(id);
        return ApiResponse.success(invoice);
    }

    @Operation(summary = "创建发票", description = "根据请求参数创建一张新的发票记录。")
    @PostMapping
    public ApiResponse<Invoice> create(@RequestBody @Valid CreateInvoiceRequest request) {
        Invoice invoice = invoiceService.create(request);
        return ApiResponse.success(invoice);
    }

    @Operation(summary = "更新发票", description = "按发票 ID 更新发票中允许修改的开票信息。")
    @PutMapping("/{id}")
    public ApiResponse<Invoice> update(
            @Parameter(description = "发票 ID") @PathVariable String id,
            @RequestBody UpdateInvoiceRequest request) {
        Invoice invoice = invoiceService.update(id, request);
        return ApiResponse.success(invoice);
    }

    @Operation(summary = "删除发票", description = "按发票 ID 删除对应的发票记录。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "发票 ID") @PathVariable String id) {
        invoiceService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询发票", description = "按往来方 ID 分页查询该往来方名下的发票列表。")
    @GetMapping("/page")
    public ApiResponse<Page<Invoice>> pageByPartyId(
            @Parameter(description = "往来方 ID，即开票对象（客户或供应商）") @RequestParam String partyId,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize) {
        Page<Invoice> page = invoiceService.pageByPartyId(partyId, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @Operation(summary = "开具发票", description = "按发票 ID 与开票人姓名将发票置为已开具状态。")
    @PostMapping("/{id}/issue")
    public ApiResponse<Invoice> issue(
            @Parameter(description = "发票 ID") @PathVariable String id,
            @Parameter(description = "开票人姓名") @RequestParam String issuerName) {
        Invoice invoice = invoiceService.issueInvoice(id, issuerName);
        return ApiResponse.success(invoice);
    }

    @Operation(summary = "作废发票", description = "将指定发票作废，作废后该发票不可继续使用。")
    @PostMapping("/{id}/void")
    public ApiResponse<Invoice> voidInvoice(@Parameter(description = "发票 ID") @PathVariable String id) {
        Invoice invoice = invoiceService.voidInvoice(id);
        return ApiResponse.success(invoice);
    }

    @Operation(summary = "发票红冲", description = "对指定发票执行红冲，生成用于冲销原发票的负数记录。")
    @PostMapping("/{id}/red-flush")
    public ApiResponse<Invoice> redFlush(@Parameter(description = "发票 ID") @PathVariable String id) {
        Invoice invoice = invoiceService.redFlushInvoice(id);
        return ApiResponse.success(invoice);
    }
}
