package com.cognora.lms.logistics.controller;

import com.cognora.lms.logistics.service.ITmsCarrierService;
import com.cognora.lms.logistics.service.ITmsDeliveryAreaService;
import com.cognora.lms.logistics.service.ITmsRouteService;
import com.cognora.lms.logistics.service.ITmsWaybillService;
import com.cognora.platform.common.security.handler.SecurityRestExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LogisticsCrudControllersTest {

    @Mock
    private ITmsCarrierService carrierService;
    @Mock
    private ITmsDeliveryAreaService deliveryAreaService;
    @Mock
    private ITmsRouteService routeService;
    @Mock
    private ITmsWaybillService waybillService;

    @InjectMocks
    private TmsCarrierController carrierController;
    @InjectMocks
    private TmsDeliveryAreaController deliveryAreaController;
    @InjectMocks
    private TmsRouteController routeController;
    @InjectMocks
    private TmsWaybillController waybillController;

    @Test
    void carrierCreateDeleteAndToggleShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(carrierController).build();

        mvc.perform(post("/api/v1/tms-carrier").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(carrierService).createCarrier(any());

        mvc.perform(delete("/api/v1/tms-carrier/carrier-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(carrierService).deleteCarrier("carrier-1");

        mvc.perform(put("/api/v1/tms-carrier/carrier-1/enable").param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(carrierService).toggleEnabled("carrier-1", false);
    }

    @Test
    void deliveryAreaCreateAndDeleteShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(deliveryAreaController).build();

        mvc.perform(post("/api/v1/tms-delivery-area").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(deliveryAreaService).createArea(any());

        mvc.perform(delete("/api/v1/tms-delivery-area/area-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(deliveryAreaService).deleteArea("area-1");
    }

    @Test
    void routeCreateShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(routeController).build();

        mvc.perform(post("/api/v1/tms-route").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(routeService).createRoute(any());
    }

    @Test
    void waybillDeleteShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(waybillController).build();

        mvc.perform(delete("/api/v1/tms-waybill/waybill-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(waybillService).deleteWaybill("waybill-1");
    }

    @Test
    void crossTenantWaybillMutationShouldReturn403() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(waybillController)
                .setControllerAdvice(new SecurityRestExceptionHandler())
                .build();
        when(waybillService.updateStatus(eq("waybill-1"), eq(4), any()))
                .thenThrow(new AccessDeniedException("Waybill is not accessible in the current tenant"));

        mvc.perform(put("/api/v1/tms-waybill/waybill-1/status").param("status", "4"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}