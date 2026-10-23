package com.scmcloud.analytics.controller;

import com.scmcloud.analytics.api.dto.DatasetQueryRequest;
import com.scmcloud.analytics.api.dto.DatasetQueryResult;
import com.scmcloud.analytics.query.QueryEngine;
import com.scmcloud.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private QueryEngine queryEngine;

    @InjectMocks
    private AnalyticsQueryController controller;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void returnsQueryResult() throws Exception {
        DatasetQueryResult result = new DatasetQueryResult(
                "sales",
                List.of(new DatasetQueryResult.Column("pay_amount", "DECIMAL")),
                List.of(Map.of("pay_amount", 100)),
                1,
                10,
                5);
        when(queryEngine.query(any(DatasetQueryRequest.class))).thenReturn(result);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mockMvc.perform(post("/api/v1/analytics/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "datasetCode": "sales",
                                  "dimensions": ["order_date"],
                                  "metrics": ["pay_amount"],
                                  "limit": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.datasetCode").value("sales"))
                .andExpect(jsonPath("$.data.columns[0].code").value("pay_amount"))
                .andExpect(jsonPath("$.data.rows[0].pay_amount").value(100));
    }

    @Test
    void passesOrganizationScopeFromHeader() throws Exception {
        TenantContextHolder.setTenantId(TENANT_ID);
        DatasetQueryResult result = new DatasetQueryResult(
                "sales", List.of(), List.of(), 0, 10, 1);
        when(queryEngine.query(any(DatasetQueryRequest.class), eq(TENANT_ID),
                eq(List.of("org-1", "org-2")))).thenReturn(result);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mockMvc.perform(post("/api/v1/analytics/query")
                        .header("X-Organization-Ids", "org-1, org-2,org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"datasetCode\":\"sales\",\"dimensions\":[\"order_date\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rowCount").value(0));

        verify(queryEngine).query(any(DatasetQueryRequest.class), eq(TENANT_ID),
                eq(List.of("org-1", "org-2")));
    }
}
