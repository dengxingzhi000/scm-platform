package com.cognora.scm.finance.controller;

import com.cognora.scm.finance.service.IFreightRuleService;
import com.cognora.scm.finance.service.IPlatformServiceFeeService;
import com.cognora.scm.finance.service.IReconciliationRecordService;
import com.cognora.scm.finance.service.ISettlementItemService;
import com.cognora.scm.finance.service.ISettlementOrderService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceCrudControllersTest {

    @Mock
    private IFreightRuleService freightRuleService;
    @Mock
    private ISettlementOrderService settlementOrderService;
    @Mock
    private IReconciliationRecordService reconciliationRecordService;
    @Mock
    private IPlatformServiceFeeService platformServiceFeeService;
    @Mock
    private ISettlementItemService settlementItemService;

    @InjectMocks
    private FreightRuleController freightRuleController;
    @InjectMocks
    private SettlementOrderController settlementOrderController;
    @InjectMocks
    private ReconciliationRecordController reconciliationRecordController;
    @InjectMocks
    private PlatformServiceFeeController platformServiceFeeController;
    @InjectMocks
    private SettlementItemController settlementItemController;

    @Test
    void freightRuleCreateAndDeleteShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(freightRuleController).build();

        mvc.perform(post("/freight-rule").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(freightRuleService).createRule(any());

        mvc.perform(delete("/freight-rule/rule-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(freightRuleService).deleteRule("rule-1");
    }

    @Test
    void settlementOrderUpdateAndDeleteShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(settlementOrderController).build();

        mvc.perform(put("/settlement-order/order-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(settlementOrderService).updateSettlement(any(), any());

        mvc.perform(delete("/settlement-order/order-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(settlementOrderService).deleteSettlement("order-1");
    }

    @Test
    void reconciliationUpdateAndDeleteShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(reconciliationRecordController).build();

        mvc.perform(put("/reconciliation-record/rec-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(reconciliationRecordService).updateReconciliation(any(), any());

        mvc.perform(delete("/reconciliation-record/rec-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(reconciliationRecordService).deleteReconciliation("rec-1");
    }

    @Test
    void platformServiceFeeCreateShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(platformServiceFeeController).build();

        mvc.perform(post("/platform-service-fee").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(platformServiceFeeService).createFee(any());
    }

    @Test
    void settlementItemCreateShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(settlementItemController).build();

        mvc.perform(post("/settlement-item").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(settlementItemService).createItem(any());
    }
}