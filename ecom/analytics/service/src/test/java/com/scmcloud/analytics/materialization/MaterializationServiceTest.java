package com.scmcloud.analytics.materialization;

import com.scmcloud.analytics.config.ClickHouseProperties;
import com.scmcloud.analytics.query.ClickHouseExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterializationServiceTest {

    private static final String TENANT_A = "11111111-1111-1111-1111-111111111111";
    private static final String TENANT_B = "22222222-2222-2222-2222-222222222222";
    private static final LocalDate DATE = LocalDate.of(2026, 10, 22);

    @Mock
    private ClickHouseExecutor executor;

    private MaterializationService service;

    @BeforeEach
    void setUp() {
        ClickHouseProperties properties = new ClickHouseProperties();
        properties.setQueryTimeoutSeconds(30);
        service = new MaterializationService(executor, properties);
    }

    @Test
    void materializesDwdPerTenantWithDeleteFirst() {
        when(executor.query(anyString(), anyList(), anyInt()))
                .thenReturn(List.of(Map.of("tenant_id", TENANT_A), Map.of("tenant_id", TENANT_B)));

        int tenants = service.materializeDwd(DATE, 0, 1);

        assertEquals(2, tenants);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(executor, times(4)).execute(sql.capture(), eq(30));
        assertTrue(sql.getAllValues().get(0).startsWith("DELETE FROM analytics.dwd_sales_detail"));
        assertTrue(sql.getAllValues().get(0).contains(TENANT_A));
        assertTrue(sql.getAllValues().get(0).contains("toDate('2026-10-22')"));
        assertTrue(sql.getAllValues().get(1).startsWith("INSERT INTO analytics.dwd_sales_detail"));
        assertTrue(sql.getAllValues().get(1).contains("FROM analytics.ods_order FINAL"));
        assertTrue(sql.getAllValues().get(2).contains(TENANT_B));
    }

    @Test
    void materializesDwsAndAdsForEachTenant() {
        when(executor.query(anyString(), anyList(), anyInt()))
                .thenReturn(List.of(Map.of("tenant_id", TENANT_A)));

        assertEquals(1, service.materializeDws(DATE, 0, 1));
        verify(executor, times(4)).execute(anyString(), eq(30));

        assertEquals(1, service.materializeAds(DATE, 0, 1));
        verify(executor, times(8)).execute(anyString(), eq(30));
    }

    @Test
    void skipsMalformedTenantIds() {
        when(executor.query(anyString(), anyList(), anyInt()))
                .thenReturn(List.of(Map.of("tenant_id", TENANT_A), Map.of("tenant_id", "not-a-uuid")));

        assertEquals(1, service.materializeDwd(DATE, 0, 1));
    }

    @Test
    void shardsTenantsDeterministically() {
        List<String> tenants = List.of(TENANT_A, TENANT_B, "33333333-3333-3333-3333-333333333333");

        List<String> shard0 = MaterializationService.shard(tenants, 0, 2);
        List<String> shard1 = MaterializationService.shard(tenants, 1, 2);

        assertEquals(tenants.size(), shard0.size() + shard1.size());
        assertFalse(shard0.stream().anyMatch(shard1::contains));
        assertEquals(tenants, MaterializationService.shard(tenants, 0, 1));
    }

    @Test
    void parsesJobParameterDate() {
        assertEquals(DATE, MaterializationService.resolveDate("2026-10-22"));
        assertEquals(DATE, MaterializationService.resolveDate("date=2026-10-22"));
        assertEquals(DATE, MaterializationService.resolveDate("2026-10-22,tenant=ignored"));
    }
}
