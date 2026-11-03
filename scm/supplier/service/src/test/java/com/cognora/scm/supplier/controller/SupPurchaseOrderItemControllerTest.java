package com.cognora.scm.supplier.controller;

import com.cognora.scm.supplier.domain.entity.SupPurchaseOrderItem;
import com.cognora.scm.supplier.service.ISupPurchaseOrderItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SupPurchaseOrderItemControllerTest {

    @Mock
    private ISupPurchaseOrderItemService purchaseOrderItemService;

    @InjectMocks
    private SupPurchaseOrderItemController controller;

    @Test
    void createShouldDelegateEntirelyToService() throws Exception {
        when(purchaseOrderItemService.createItem(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/api/v1/purchase-order-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\":12.34,\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SupPurchaseOrderItem> captor = ArgumentCaptor.forClass(SupPurchaseOrderItem.class);
        verify(purchaseOrderItemService).createItem(captor.capture());
        assertEquals(new BigDecimal("12.34"), captor.getValue().getUnitPrice());
        assertEquals(3, captor.getValue().getQuantity());
    }

    @Test
    void updateShouldReturn404WhenServiceFindsNothing() throws Exception {
        when(purchaseOrderItemService.updateItem(any(), any())).thenReturn(null);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(put("/api/v1/purchase-order-items/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void updateShouldReturnServiceResult() throws Exception {
        SupPurchaseOrderItem updated = new SupPurchaseOrderItem();
        updated.setSubtotal(new BigDecimal("20.00"));
        when(purchaseOrderItemService.updateItem(any(), any())).thenReturn(updated);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(put("/api/v1/purchase-order-items/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.subtotal").value(20.00));
    }
}