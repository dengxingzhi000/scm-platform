package com.scmcloud.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.finance.domain.dto.CreateInvoiceRequest;
import com.scmcloud.finance.domain.dto.UpdateInvoiceRequest;
import com.scmcloud.finance.domain.entity.Invoice;
import com.scmcloud.finance.domain.enums.InvoiceStatus;
import com.scmcloud.finance.service.IInvoiceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private IInvoiceService invoiceService;

    @InjectMocks
    private InvoiceController invoiceController;

    private MockMvc mockMvc;

    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invoiceController).build();
    }

    @Test
    void shouldGetById() throws Exception {
        // Given
        String id = "invoice-1";
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setInvoiceNo("INV-2026-001");

        when(invoiceService.getById(id)).thenReturn(invoice);

        // When & Then
        mockMvc.perform(get("/invoice/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.invoiceNo").value("INV-2026-001"));
    }

    @Test
    void shouldCreate() throws Exception {
        // Given
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setInvoiceNo("INV-2026-001");
        request.setInvoiceType(1);
        request.setInvoiceKind(1);
        request.setPartyId("party-1");

        Invoice created = new Invoice();
        created.setId("new-id");
        created.setInvoiceNo("INV-2026-001");

        when(invoiceService.create(any(CreateInvoiceRequest.class))).thenReturn(created);

        // When & Then
        mockMvc.perform(post("/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceNo\":\"INV-2026-001\",\"invoiceType\":1,\"invoiceKind\":1,\"partyId\":\"party-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("new-id"))
                .andExpect(jsonPath("$.invoiceNo").value("INV-2026-001"));
    }

    @Test
    void shouldUpdate() throws Exception {
        // Given
        String id = "invoice-1";
        UpdateInvoiceRequest request = new UpdateInvoiceRequest();
        request.setInvoiceNo("INV-2026-001-UPDATED");

        Invoice updated = new Invoice();
        updated.setId(id);
        updated.setInvoiceNo("INV-2026-001-UPDATED");

        when(invoiceService.update(eq(id), any(UpdateInvoiceRequest.class))).thenReturn(updated);

        // When & Then
        mockMvc.perform(put("/invoice/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceNo\":\"INV-2026-001-UPDATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNo").value("INV-2026-001-UPDATED"));
    }

    @Test
    void shouldDelete() throws Exception {
        // Given
        String id = "invoice-1";
        doNothing().when(invoiceService).delete(id);

        // When & Then
        mockMvc.perform(delete("/invoice/{id}", id))
                .andExpect(status().isOk());
        verify(invoiceService).delete(id);
    }

    @Test
    void shouldPageByPartyId() throws Exception {
        // Given
        String partyId = "party-1";
        Page<Invoice> page = new Page<>();
        page.setRecords(Arrays.asList(new Invoice(), new Invoice()));
        page.setTotal(2);

        when(invoiceService.pageByPartyId(eq(partyId), anyInt(), anyInt())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/invoice/page")
                        .param("partyId", partyId)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void shouldIssue() throws Exception {
        // Given
        String id = "invoice-1";
        String issuerName = "John";

        Invoice issued = new Invoice();
        issued.setId(id);
        issued.setStatus(InvoiceStatus.ISSUED.getCode());

        when(invoiceService.issueInvoice(id, issuerName)).thenReturn(issued);

        // When & Then
        mockMvc.perform(post("/invoice/{id}/issue", id)
                        .param("issuerName", issuerName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(InvoiceStatus.ISSUED.getCode()));
    }

    @Test
    void shouldVoidInvoice() throws Exception {
        // Given
        String id = "invoice-1";

        Invoice voided = new Invoice();
        voided.setId(id);
        voided.setStatus(InvoiceStatus.VOIDED.getCode());

        when(invoiceService.voidInvoice(id)).thenReturn(voided);

        // When & Then
        mockMvc.perform(post("/invoice/{id}/void", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(InvoiceStatus.VOIDED.getCode()));
    }

    @Test
    void shouldRedFlush() throws Exception {
        // Given
        String id = "invoice-1";

        Invoice redFlushed = new Invoice();
        redFlushed.setId(id);
        redFlushed.setStatus(InvoiceStatus.RED_FLUSHED.getCode());

        when(invoiceService.redFlushInvoice(id)).thenReturn(redFlushed);

        // When & Then
        mockMvc.perform(post("/invoice/{id}/red-flush", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(InvoiceStatus.RED_FLUSHED.getCode()));
    }
}
