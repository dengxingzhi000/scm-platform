package com.cognora.scm.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.exception.GlobalExceptionHandler;
import com.cognora.scm.finance.domain.dto.CreateInvoiceRequest;
import com.cognora.scm.finance.domain.dto.UpdateInvoiceRequest;
import com.cognora.scm.finance.domain.entity.Invoice;
import com.cognora.scm.finance.domain.enums.InvoiceStatus;
import com.cognora.scm.finance.service.IInvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private IInvoiceService invoiceService;

    @InjectMocks
    private InvoiceController invoiceController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invoiceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldGetById() throws Exception {
        String id = "invoice-1";
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setInvoiceNo("INV-2026-001");

        when(invoiceService.getById(id)).thenReturn(invoice);

        mockMvc.perform(get("/invoice/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.invoiceNo").value("INV-2026-001"));

        verify(invoiceService).getById(id);
    }

    @Test
    void shouldCreate() throws Exception {
        Invoice created = new Invoice();
        created.setId("new-id");
        created.setInvoiceNo("INV-2026-001");

        when(invoiceService.create(any(CreateInvoiceRequest.class))).thenReturn(created);

        mockMvc.perform(post("/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceNo\":\"INV-2026-001\",\"invoiceType\":1,\"invoiceKind\":1,\"partyId\":\"party-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("new-id"))
                .andExpect(jsonPath("$.data.invoiceNo").value("INV-2026-001"));

        verify(invoiceService).create(any(CreateInvoiceRequest.class));
    }

    @Test
    void shouldUpdate() throws Exception {
        String id = "invoice-1";
        Invoice updated = new Invoice();
        updated.setId(id);
        updated.setInvoiceNo("INV-2026-001-UPDATED");

        when(invoiceService.update(eq(id), any(UpdateInvoiceRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/invoice/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceNo\":\"INV-2026-001-UPDATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.invoiceNo").value("INV-2026-001-UPDATED"));

        verify(invoiceService).update(eq(id), any(UpdateInvoiceRequest.class));
    }

    @Test
    void shouldDelete() throws Exception {
        String id = "invoice-1";

        mockMvc.perform(delete("/invoice/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(invoiceService).delete(id);
    }

    @Test
    void shouldPageByPartyId() throws Exception {
        String partyId = "party-1";
        Page<Invoice> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(new Invoice(), new Invoice()));

        when(invoiceService.pageByPartyId(eq(partyId), eq(1), eq(10))).thenReturn(page);

        mockMvc.perform(get("/invoice/page")
                        .param("partyId", partyId)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2));

        verify(invoiceService).pageByPartyId(eq(partyId), eq(1), eq(10));
    }

    @Test
    void shouldIssue() throws Exception {
        String id = "invoice-1";
        String issuerName = "John";

        Invoice issued = new Invoice();
        issued.setId(id);
        issued.setStatus(InvoiceStatus.ISSUED.getCode());

        when(invoiceService.issueInvoice(id, issuerName)).thenReturn(issued);

        mockMvc.perform(post("/invoice/{id}/issue", id)
                        .param("issuerName", issuerName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(InvoiceStatus.ISSUED.getCode()));

        verify(invoiceService).issueInvoice(id, issuerName);
    }

    @Test
    void shouldVoidInvoice() throws Exception {
        String id = "invoice-1";

        Invoice voided = new Invoice();
        voided.setId(id);
        voided.setStatus(InvoiceStatus.VOIDED.getCode());

        when(invoiceService.voidInvoice(id)).thenReturn(voided);

        mockMvc.perform(post("/invoice/{id}/void", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(InvoiceStatus.VOIDED.getCode()));

        verify(invoiceService).voidInvoice(id);
    }

    @Test
    void shouldRedFlush() throws Exception {
        String id = "invoice-1";

        Invoice redFlushed = new Invoice();
        redFlushed.setId(id);
        redFlushed.setStatus(InvoiceStatus.RED_FLUSHED.getCode());

        when(invoiceService.redFlushInvoice(id)).thenReturn(redFlushed);

        mockMvc.perform(post("/invoice/{id}/red-flush", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(InvoiceStatus.RED_FLUSHED.getCode()));

        verify(invoiceService).redFlushInvoice(id);
    }

    @Test
    void createRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceType\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.invoiceNo").value("发票号码不能为空"))
                .andExpect(jsonPath("$.data.invoiceKind").value("发票种类不能为空"))
                .andExpect(jsonPath("$.data.partyId").value("往来方ID不能为空"));

        verifyNoInteractions(invoiceService);
    }

    @Test
    void getByIdReturnsBadRequestWhenInvoiceMissing() throws Exception {
        when(invoiceService.getById("missing"))
                .thenThrow(new IllegalArgumentException("Invoice not found: missing"));

        mockMvc.perform(get("/invoice/{id}", "missing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid argument"));

        verify(invoiceService).getById("missing");
    }
}
