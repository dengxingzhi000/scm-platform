package com.scmcloud.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.finance.domain.dto.CreateInvoiceRequest;
import com.scmcloud.finance.domain.dto.UpdateInvoiceRequest;
import com.scmcloud.finance.domain.entity.Invoice;
import com.scmcloud.finance.service.IInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/invoice")
public class InvoiceController {
    private final IInvoiceService invoiceService;

    @GetMapping("/{id}")
    public ApiResponse<Invoice> getById(@PathVariable String id) {
        Invoice invoice = invoiceService.getById(id);
        return ApiResponse.success(invoice);
    }

    @PostMapping
    public ApiResponse<Invoice> create(@RequestBody @Valid CreateInvoiceRequest request) {
        Invoice invoice = invoiceService.create(request);
        return ApiResponse.success(invoice);
    }

    @PutMapping("/{id}")
    public ApiResponse<Invoice> update(
            @PathVariable String id,
            @RequestBody UpdateInvoiceRequest request) {
        Invoice invoice = invoiceService.update(id, request);
        return ApiResponse.success(invoice);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        invoiceService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/page")
    public ApiResponse<Page<Invoice>> pageByPartyId(
            @RequestParam String partyId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Invoice> page = invoiceService.pageByPartyId(partyId, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @PostMapping("/{id}/issue")
    public ApiResponse<Invoice> issue(
            @PathVariable String id,
            @RequestParam String issuerName) {
        Invoice invoice = invoiceService.issueInvoice(id, issuerName);
        return ApiResponse.success(invoice);
    }

    @PostMapping("/{id}/void")
    public ApiResponse<Invoice> voidInvoice(@PathVariable String id) {
        Invoice invoice = invoiceService.voidInvoice(id);
        return ApiResponse.success(invoice);
    }

    @PostMapping("/{id}/red-flush")
    public ApiResponse<Invoice> redFlush(@PathVariable String id) {
        Invoice invoice = invoiceService.redFlushInvoice(id);
        return ApiResponse.success(invoice);
    }
}
