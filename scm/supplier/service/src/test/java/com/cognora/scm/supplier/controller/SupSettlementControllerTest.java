package com.cognora.scm.supplier.controller;

import com.cognora.scm.supplier.domain.entity.SupSettlement;
import com.cognora.scm.supplier.service.ISupSettlementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SupSettlementControllerTest {

    @Mock
    private ISupSettlementService settlementService;

    @InjectMocks
    private SupSettlementController controller;

    @Test
    void createShouldDelegateToService() throws Exception {
        when(settlementService.createSettlement(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/api/v1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":\"supplier-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settlementService).createSettlement(any());
    }

    @Test
    void updateShouldReturn404WhenServiceFindsNothing() throws Exception {
        when(settlementService.updateSettlement(any(), any())).thenReturn(null);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(put("/api/v1/settlements/{id}", "missing")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void updateShouldReturnServiceResult() throws Exception {
        SupSettlement updated = new SupSettlement();
        updated.setStatus(1);
        when(settlementService.updateSettlement(any(), any())).thenReturn(updated);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(put("/api/v1/settlements/{id}", "settlement-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    void deleteShouldReturn404WhenServiceReportsMissing() throws Exception {
        when(settlementService.deleteSettlement(any())).thenReturn(false);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(delete("/api/v1/settlements/{id}", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}