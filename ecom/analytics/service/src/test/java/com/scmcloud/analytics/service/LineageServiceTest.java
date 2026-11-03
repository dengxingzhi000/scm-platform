package com.scmcloud.analytics.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.scmcloud.analytics.domain.entity.LineageEdge;
import com.scmcloud.analytics.mapper.LineageEdgeMapper;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineageServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private LineageEdgeMapper lineageEdgeMapper;

    private LineageService lineageService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        lineageService = new LineageService(lineageEdgeMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void recordsNewEdgeWithTenant() {
        when(lineageEdgeMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(lineageEdgeMapper.insert(any(LineageEdge.class))).thenReturn(1);

        LineageEdge edge = LineageEdge.builder()
                .upstreamNode("analytics.ods_order")
                .downstreamNode("analytics.dwd_sales_detail")
                .edgeType("TABLE")
                .jobName("analyticsDwdJob")
                .build();

        LineageEdge result = lineageService.record(edge);

        ArgumentCaptor<LineageEdge> captor = ArgumentCaptor.forClass(LineageEdge.class);
        verify(lineageEdgeMapper).insert(captor.capture());
        assertEquals(TENANT_ID.toString(), captor.getValue().getTenantId());
        assertEquals("TABLE", result.getEdgeType());
        assertEquals(false, result.getDeleted());
    }

    @Test
    void updatesExistingEdgeInsteadOfDuplicating() {
        LineageEdge existing = LineageEdge.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID.toString())
                .upstreamNode("analytics.ods_order")
                .downstreamNode("analytics.dwd_sales_detail")
                .edgeType("TABLE")
                .build();
        when(lineageEdgeMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        LineageEdge result = lineageService.record(LineageEdge.builder()
                .upstreamNode("analytics.ods_order")
                .downstreamNode("analytics.dwd_sales_detail")
                .edgeType("TABLE")
                .jobName("newJob")
                .build());

        assertEquals("newJob", result.getJobName());
        verify(lineageEdgeMapper).updateById(existing);
    }

    @Test
    void rejectsSelfReference() {
        assertThrows(BusinessException.class, () -> lineageService.record(LineageEdge.builder()
                .upstreamNode("analytics.ods_order")
                .downstreamNode("analytics.ods_order")
                .build()));
    }

    @Test
    void rejectsUnsafeNodeName() {
        assertThrows(BusinessException.class, () -> lineageService.record(LineageEdge.builder()
                .upstreamNode("analytics.ods_order; DROP TABLE x")
                .downstreamNode("analytics.dwd_sales_detail")
                .build()));
    }

    @Test
    void delegatesUpstreamLookup() {
        when(lineageEdgeMapper.selectUpstream(TENANT_ID.toString(), "analytics.dws_sales_day"))
                .thenReturn(List.of());

        assertEquals(List.of(), lineageService.upstream("analytics.dws_sales_day"));
        verify(lineageEdgeMapper).selectUpstream(TENANT_ID.toString(), "analytics.dws_sales_day");
    }
}
