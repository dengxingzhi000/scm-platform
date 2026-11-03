package com.cognora.scm.order.service.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.scm.order.domain.entity.OrdOrder;
import com.cognora.scm.order.domain.entity.OrderStatus;
import com.cognora.scm.order.dto.OrderDashboardCompare;
import com.cognora.scm.order.dto.OrderDashboardRange;
import com.cognora.scm.order.dto.OrderDashboardSummaryResponse;
import com.cognora.scm.order.mapper.OrdOrderMapper;
import com.cognora.scm.order.util.SqlTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 订单看板聚合查询。
 *
 * <p>聚合在数据库侧完成，时间范围最多 30 天，避免前端翻页扫描订单。
 * 营收口径：已支付及之后、且未取消/退款的状态。今日按小时聚合，
 * 7/30 天按日聚合；对比周期与当前周期等长（今日对比昨日同一时段）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdOrderDashboardQueryService {

    private static final Set<Integer> REVENUE_STATUS_CODES = Set.of(
            OrderStatus.PAID.getCode(),
            OrderStatus.PENDING_SHIP.getCode(),
            OrderStatus.SHIPPED.getCode(),
            OrderStatus.IN_TRANSIT.getCode(),
            OrderStatus.DELIVERED.getCode(),
            OrderStatus.COMPLETED.getCode()
    );

    private final OrdOrderMapper ordOrderMapper;

    @Slave
    public OrderDashboardSummaryResponse summary(OrderDashboardRange range, OrderDashboardCompare compare) {
        LocalDate today = SqlTime.today();
        LocalDateTime rangeStart;
        LocalDateTime rangeEnd;
        Granularity granularity;

        if (range == OrderDashboardRange.TODAY) {
            rangeStart = today.atStartOfDay();
            // Include the current second so an order created at 00:00:00 is not lost.
            rangeEnd = SqlTime.now().plusSeconds(1);
            granularity = Granularity.HOUR;
        } else {
            rangeStart = today.minusDays(range.getDays() - 1L).atStartOfDay();
            rangeEnd = today.plusDays(1).atStartOfDay();
            granularity = Granularity.DAY;
        }

        RangeAggregate current = aggregate(rangeStart, rangeEnd, granularity);
        ComparisonWindow window = comparisonWindow(range, compare, rangeStart, rangeEnd);
        RangeAggregate comparison = window == null
                ? RangeAggregate.empty()
                : aggregate(window.start(), window.end(), granularity);

        OrderDashboardSummaryResponse response = new OrderDashboardSummaryResponse();
        response.setRange(range.name());
        response.setCompareMode(compare.name());
        response.setGranularity(granularity.name());
        response.setRangeStart(rangeStart);
        response.setRangeEnd(rangeEnd);
        response.setGeneratedAt(SqlTime.now());
        if (window != null) {
            response.setComparisonStartDate(window.start().toLocalDate());
            response.setComparisonEndDate(window.end().minusSeconds(1).toLocalDate());
        }
        response.setCurrentRevenue(current.revenue());
        response.setCurrentOrders(current.orders());
        response.setComparisonRevenue(comparison.revenue());
        response.setComparisonOrders(comparison.orders());
        response.setPoints(current.points());
        response.setStatusCounts(statusCounts(rangeStart, rangeEnd));

        Backlog backlog = backlog();
        OrderDashboardSummaryResponse.Attention attention = new OrderDashboardSummaryResponse.Attention();
        attention.setPendingPayment(backlog.pendingPayment());
        attention.setPendingShip(backlog.pendingShip());
        attention.setRefunding(backlog.refunding());
        response.setAttention(attention);
        response.setTotalOrders(backlog.totalOrders());

        log.info("[Dashboard] range={} compare={} granularity={} orders={} revenue={}",
                range, compare, granularity, current.orders(), current.revenue());
        return response;
    }

    private RangeAggregate aggregate(LocalDateTime start, LocalDateTime end, Granularity granularity) {
        String bucketExpression = granularity == Granularity.HOUR
                ? "date_trunc('hour', create_time at time zone " + SqlTime.zoneLiteral() + ")"
                : "DATE(create_time at time zone " + SqlTime.zoneLiteral() + ")";

        QueryWrapper<OrdOrder> wrapper = new QueryWrapper<>();
        wrapper.select(
                        bucketExpression + " AS bucket",
                        "COUNT(*) AS order_count",
                        revenueSumExpression() + " AS revenue")
                .eq("deleted", false)
                .apply("create_time >= ({0}::timestamptz)", SqlTime.absolute(start))
                .apply("create_time < ({0}::timestamptz)", SqlTime.absolute(end))
                .groupBy(bucketExpression)
                .orderByAsc(bucketExpression);

        List<OrderDashboardSummaryResponse.TrendPoint> points = emptyBuckets(start, end, granularity);
        long orders = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        for (Map<String, Object> row : ordOrderMapper.selectMaps(wrapper)) {
            LocalDateTime bucket = toLocalDateTime(row.get("bucket"));
            if (bucket == null) {
                continue;
            }
            long count = toLong(row.get("order_count"));
            BigDecimal amount = toDecimal(row.get("revenue"));
            orders += count;
            revenue = revenue.add(amount);
            for (OrderDashboardSummaryResponse.TrendPoint point : points) {
                if (point.getStartTime().equals(bucket)) {
                    point.setOrders(count);
                    point.setRevenue(amount);
                    break;
                }
            }
        }
        return new RangeAggregate(points, orders, revenue);
    }

    private List<OrderDashboardSummaryResponse.TrendPoint> emptyBuckets(
            LocalDateTime start, LocalDateTime end, Granularity granularity) {
        List<OrderDashboardSummaryResponse.TrendPoint> points = new ArrayList<>();
        LocalDateTime cursor = granularity == Granularity.HOUR
                ? start.truncatedTo(ChronoUnit.HOURS)
                : start.toLocalDate().atStartOfDay();

        while (cursor.isBefore(end)) {
            LocalDateTime next = granularity == Granularity.HOUR
                    ? cursor.plusHours(1)
                    : cursor.plusDays(1);
            OrderDashboardSummaryResponse.TrendPoint point = new OrderDashboardSummaryResponse.TrendPoint();
            point.setStartTime(cursor);
            point.setEndTime(next);
            points.add(point);
            cursor = next;
        }
        return points;
    }

    private List<OrderDashboardSummaryResponse.StatusCount> statusCounts(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<OrdOrder> wrapper = new QueryWrapper<>();
        wrapper.select("status AS status", "COUNT(*) AS cnt")
                .eq("deleted", false)
                .apply("create_time >= ({0}::timestamptz)", SqlTime.absolute(start))
                .apply("create_time < ({0}::timestamptz)", SqlTime.absolute(end))
                .groupBy("status")
                .orderByAsc("status");

        List<OrderDashboardSummaryResponse.StatusCount> counts = new ArrayList<>();
        for (Map<String, Object> row : ordOrderMapper.selectMaps(wrapper)) {
            int code = (int) toLong(row.get("status"));
            OrderDashboardSummaryResponse.StatusCount item = new OrderDashboardSummaryResponse.StatusCount();
            item.setStatus(OrderStatus.fromCode(code).name());
            item.setCount(toLong(row.get("cnt")));
            counts.add(item);
        }
        return counts;
    }

    private Backlog backlog() {
        QueryWrapper<OrdOrder> wrapper = new QueryWrapper<>();
        wrapper.select(
                        "COUNT(*) AS total_orders",
                        "COALESCE(SUM(CASE WHEN status = " + OrderStatus.PENDING_PAYMENT.getCode()
                                + " THEN 1 ELSE 0 END), 0) AS pending_payment",
                        "COALESCE(SUM(CASE WHEN status = " + OrderStatus.PENDING_SHIP.getCode()
                                + " THEN 1 ELSE 0 END), 0) AS pending_ship",
                        "COALESCE(SUM(CASE WHEN status = " + OrderStatus.REFUNDING.getCode()
                                + " THEN 1 ELSE 0 END), 0) AS refunding")
                .eq("deleted", false);

        List<Map<String, Object>> rows = ordOrderMapper.selectMaps(wrapper);
        if (rows.isEmpty()) {
            return new Backlog(0, 0, 0, 0);
        }
        Map<String, Object> row = rows.get(0);
        return new Backlog(
                toLong(row.get("pending_payment")),
                toLong(row.get("pending_ship")),
                toLong(row.get("refunding")),
                toLong(row.get("total_orders"))
        );
    }

    private ComparisonWindow comparisonWindow(
            OrderDashboardRange range, OrderDashboardCompare compare,
            LocalDateTime start, LocalDateTime end) {
        if (compare == OrderDashboardCompare.NONE) {
            return null;
        }
        if (compare == OrderDashboardCompare.YEAR_OVER_YEAR) {
            return new ComparisonWindow(start.minusYears(1), end.minusYears(1));
        }
        if (range == OrderDashboardRange.TODAY) {
            LocalDateTime previousStart = start.minusDays(1);
            return new ComparisonWindow(previousStart, previousStart.plus(Duration.between(start, end)));
        }
        return new ComparisonWindow(start.minusDays(range.getDays()), end.minusDays(range.getDays()));
    }

    private String revenueSumExpression() {
        return "COALESCE(SUM(CASE WHEN status IN (" + String.join(",",
                REVENUE_STATUS_CODES.stream().map(String::valueOf).toList())
                + ") THEN payable_amount ELSE 0 END), 0)";
    }

    private static long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        // java.sql.Date extends java.util.Date but its toInstant() is unsupported.
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().atStartOfDay();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (value instanceof LocalDate day) {
            return day.atStartOfDay();
        }
        return value == null ? null : LocalDateTime.parse(value.toString().replace(" ", "T"));
    }

    private enum Granularity {
        HOUR, DAY
    }

    private record ComparisonWindow(LocalDateTime start, LocalDateTime end) {
    }

    private record RangeAggregate(
            List<OrderDashboardSummaryResponse.TrendPoint> points,
            long orders,
            BigDecimal revenue) {

        private static RangeAggregate empty() {
            return new RangeAggregate(List.of(), 0, BigDecimal.ZERO);
        }
    }

    private record Backlog(long pendingPayment, long pendingShip, long refunding, long totalOrders) {
    }
}
