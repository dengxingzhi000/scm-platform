package scm.finance.controller;

import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.finance.domain.entity.Invoice;
import scm.finance.service.IInvoiceService;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
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
    public ApiResponse<Invoice> update(@PathVariable String id, @RequestBody Invoice invoice) {
        invoice.setId(id);
        invoice.setUpdateTime(LocalDateTime.now());
        invoiceService.updateById(invoice);
        log.info("发票更新成功: id={}", id);
        return ApiResponse.success(invoice);
    }

    @DeleteMapping("/{id}")
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
    public ApiResponse<List<Invoice>> listByPartyId(
            @RequestParam String partyId) {
        List<Invoice> invoices = invoiceService.listByPartyId(partyId);
        return ApiResponse.success(invoices);
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
