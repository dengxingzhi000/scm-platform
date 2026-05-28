package scm.finance.controller;

import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.finance.domain.entity.Invoice;
import scm.finance.service.IInvoiceService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/invoice")
@Tag(name = "发票管理", description = "发票管理接口")
public class InvoiceController {

    @Autowired
    private IInvoiceService invoiceService;

    @GetMapping("/{id}")
    @Operation(summary = "查询发票详情")
    public ApiResponse<Invoice> getById(@PathVariable String id) {
        Invoice invoice = invoiceService.getById(id);
        return ApiResponse.success(invoice);
    }

    @PostMapping
    @Operation(summary = "创建发票")
    public ApiResponse<Invoice> create(@RequestBody Invoice invoice) {
        invoice.setId(UUIDv7Util.generateString());
        invoice.setStatus(0);
        invoice.setDeleted(false);
        invoice.setCreateTime(LocalDateTime.now());
        invoice.setUpdateTime(LocalDateTime.now());
        invoiceService.save(invoice);
        log.info("发票创建成功: id={}, invoiceNo={}", invoice.getId(), invoice.getInvoiceNo());
        return ApiResponse.success(invoice);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新发票")
    public ApiResponse<Invoice> update(@PathVariable String id, @RequestBody Invoice invoice) {
        invoice.setId(id);
        invoice.setUpdateTime(LocalDateTime.now());
        invoiceService.updateById(invoice);
        log.info("发票更新成功: id={}", id);
        return ApiResponse.success(invoice);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除发票")
    public ApiResponse<Void> delete(@PathVariable String id) {
        Invoice invoice = invoiceService.getById(id);
        if (invoice != null) {
            invoice.setDeleted(true);
            invoice.setUpdateTime(LocalDateTime.now());
            invoiceService.updateById(invoice);
            log.info("发票删除成功: id={}", id);
        }
        return ApiResponse.success();
    }

    @GetMapping("/by-party")
    @Operation(summary = "按往来方查询发票列表")
    public ApiResponse<List<Invoice>> listByPartyId(
            @Parameter(description = "往来方ID", required = true) @RequestParam String partyId) {
        List<Invoice> invoices = invoiceService.listByPartyId(partyId);
        return ApiResponse.success(invoices);
    }

    @PostMapping("/{id}/issue")
    @Operation(summary = "开具发票")
    public ApiResponse<Invoice> issue(
            @PathVariable String id,
            @Parameter(description = "开票人姓名", required = true) @RequestParam String issuerName) {
        Invoice invoice = invoiceService.issueInvoice(id, issuerName);
        return ApiResponse.success(invoice);
    }

    @PostMapping("/{id}/void")
    @Operation(summary = "作废发票")
    public ApiResponse<Invoice> voidInvoice(@PathVariable String id) {
        Invoice invoice = invoiceService.voidInvoice(id);
        return ApiResponse.success(invoice);
    }

    @PostMapping("/{id}/red-flush")
    @Operation(summary = "红冲发票")
    public ApiResponse<Invoice> redFlush(@PathVariable String id) {
        Invoice invoice = invoiceService.redFlushInvoice(id);
        return ApiResponse.success(invoice);
    }
}
