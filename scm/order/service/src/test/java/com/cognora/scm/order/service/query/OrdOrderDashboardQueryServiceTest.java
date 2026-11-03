package com.cognora.scm.order.service.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cognora.scm.order.domain.entity.OrdOrder;
import com.cognora.scm.order.dto.OrderDashboardCompare;
import com.cognora.scm.order.dto.OrderDashboardRange;
import com.cognora.scm.order.dto.OrderDashboardSummaryResponse;
import com.cognora.scm.order.mapper.OrdOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdOrderDashboardQueryServiceTest {

    @Mock
    private OrdOrderMapper ordOrderMapper;

    @Test
    void aggregatesRangeComparisonStatusAndBacklog() {
        OrdOrderDashboardQueryService service = new OrdOrderDashboardQueryService(ordOrderMapper);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);

        // Order of selectMaps calls: current range, comparison range, status counts, backlog.
        when(ordOrderMapper.selectMaps(any()))
                .thenReturn(List.of(
                        row("bucket", java.sql.Date.valueOf(today.minusDays(2)), "order_count", 2L,
                                "revenue", new BigDecimal("1500.00")),
                        row("bucket", java.sql.Date.valueOf(today), "order_count", 1L,
                                "revenue", new BigDecimal("500.00"))))
                .thenReturn(List.of(
                        row("bucket", java.sql.Date.valueOf(start.minusDays(2)), "order_count", 1L,
                                "revenue", new BigDecimal("1000.00"))))
                .thenReturn(List.of(
                        row("status", 0, "cnt", 3L),
                        row("status", 5, "cnt", 2L)))
                .thenReturn(List.of(
                        row("total_orders", 10L, "pending_payment", 3L,
                                "pending_ship", 2L, "refunding", 1L)));

        OrderDashboardSummaryResponse response =
                service.summary(OrderDashboardRange.LAST_7_DAYS, OrderDashboardCompare.PREVIOUS_PERIOD);

        assertEquals("LAST_7_DAYS", response.getRange());
        assertEquals("PREVIOUS_PERIOD", response.getCompareMode());
        assertEquals("DAY", response.getGranularity());
        assertEquals(0, new BigDecimal("2000.00").compareTo(response.getCurrentRevenue()));
        assertEquals(3, response.getCurrentOrders());
        assertEquals(0, new BigDecimal("1000.00").compareTo(response.getComparisonRevenue()));
        assertEquals(1, response.getComparisonOrders());
        assertEquals(10, response.getTotalOrders());

        // Every day in the range is present, with zero-filled gaps.
        assertEquals(7, response.getPoints().size());
        BigDecimal pointRevenue = response.getPoints().stream()
                .map(OrderDashboardSummaryResponse.TrendPoint::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pointOrders = response.getPoints().stream()
                .mapToLong(OrderDashboardSummaryResponse.TrendPoint::getOrders)
                .sum();
        assertEquals(0, new BigDecimal("2000.00").compareTo(pointRevenue));
        assertEquals(3, pointOrders);

        assertEquals(2, response.getStatusCounts().size());
        assertEquals("PENDING_PAYMENT", response.getStatusCounts().get(0).getStatus());
        assertEquals(3, response.getStatusCounts().get(0).getCount());
        assertEquals("DELIVERED", response.getStatusCounts().get(1).getStatus());

        assertEquals(3, response.getAttention().getPendingPayment());
        assertEquals(2, response.getAttention().getPendingShip());
        assertEquals(1, response.getAttention().getRefunding());
    }

    @Test
    void todayUsesHourlyBuckets() {
        OrdOrderDashboardQueryService service = new OrdOrderDashboardQueryService(ordOrderMapper);

        when(ordOrderMapper.selectMaps(any()))
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of());

        OrderDashboardSummaryResponse response =
                service.summary(OrderDashboardRange.TODAY, OrderDashboardCompare.NONE);

        assertEquals("HOUR", response.getGranularity());
        assertTrue(response.getPoints().size() >= 1 && response.getPoints().size() <= 24);
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getCurrentRevenue()));
        assertEquals(0, response.getCurrentOrders());
    }

    @Test
    @SuppressWarnings("unchecked")
    void timePredicatesAndBucketsAreZoneExplicit() {
        OrdOrderDashboardQueryService service = new OrdOrderDashboardQueryService(ordOrderMapper);

        ArgumentCaptor<QueryWrapper<OrdOrder>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        when(ordOrderMapper.selectMaps(any()))
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of());

        service.summary(OrderDashboardRange.LAST_7_DAYS, OrderDashboardCompare.NONE);

        verify(ordOrderMapper, times(3)).selectMaps(captor.capture());
        String select = captor.getAllValues().get(0).getSqlSelect();
        String where = captor.getAllValues().get(0).getCustomSqlSegment();
        assertTrue(select.contains("at time zone"), select);
        assertTrue(where.contains("::timestamptz"), where);
    }

    private static Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put((String) keyValues[i], keyValues[i + 1]);
        }
        return row;
    }
}
