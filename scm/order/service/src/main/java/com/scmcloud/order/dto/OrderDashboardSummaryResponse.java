package com.scmcloud.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单看板聚合响应。
 *
 * <p>一次请求返回 KPI、趋势点、状态分布与待处理计数，避免前端翻页扫描订单。
 * 趋势粒度随范围变化：今日按小时、7/30 天按日；每个点自带起止时间，便于图表下钻。</p>
 *
 * @author SCM Platform Team
 */
@Data
public class OrderDashboardSummaryResponse {

    /** 请求的时间范围，例如 {@code LAST_7_DAYS}。 */
    private String range;

    /** 对比方式，例如 {@code PREVIOUS_PERIOD} / {@code YEAR_OVER_YEAR} / {@code NONE}。 */
    private String compareMode;

    /** 趋势粒度：{@code HOUR} 或 {@code DAY}。 */
    private String granularity;

    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private LocalDate comparisonStartDate;
    private LocalDate comparisonEndDate;
    private LocalDateTime generatedAt;

    private BigDecimal currentRevenue = BigDecimal.ZERO;
    private BigDecimal comparisonRevenue = BigDecimal.ZERO;
    private long currentOrders;
    private long comparisonOrders;
    /** 全部订单数（不受时间范围限制），用于“累计”提示。 */
    private long totalOrders;

    private List<TrendPoint> points = new ArrayList<>();
    private List<StatusCount> statusCounts = new ArrayList<>();
    private Attention attention = new Attention();

    @Data
    public static class TrendPoint {
        /** 桶的起止时间（左闭右开），用于图表下钻。 */
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal revenue = BigDecimal.ZERO;
        private long orders;
    }

    @Data
    public static class StatusCount {
        /** {@link com.scmcloud.order.domain.entity.OrderStatus} 枚举名。 */
        private String status;
        private long count;
    }

    /** 当前待处理积压，不受时间范围限制。 */
    @Data
    public static class Attention {
        private long pendingPayment;
        private long pendingShip;
        private long refunding;
    }
}
