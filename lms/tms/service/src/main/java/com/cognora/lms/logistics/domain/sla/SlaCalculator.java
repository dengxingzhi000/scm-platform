package com.cognora.lms.logistics.domain.sla;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * SLA 时效判定（批次 3）。
 *
 * <p>口径：承诺时效取运单 {@code estimated_delivery}；实际签收取 {@code sign_time}（缺失时回退
 * {@code actual_delivery}）。</p>
 * <ul>
 *   <li>未签收且当前时间未超过承诺时间：{@code IN_PROGRESS}；</li>
 *   <li>未签收且当前时间超过承诺时间：{@code OVERDUE}，预警等级按超时小时数划分；</li>
 *   <li>已签收：实际签收时间 ≤ 承诺时间 {@code ON_TIME}，否则 {@code OVERDUE}；</li>
 *   <li>无承诺时间：{@code NA}，不参与考核。</li>
 * </ul>
 * <p>边界：恰好在承诺时间签收视为准时；超时 &gt; 24 小时为 CRITICAL，否则 WARN。</p>
 */
public final class SlaCalculator {

    public static final String ON_TIME = "ON_TIME";
    public static final String OVERDUE = "OVERDUE";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String NA = "NA";

    public static final String LEVEL_WARN = "WARN";
    public static final String LEVEL_CRITICAL = "CRITICAL";

    /** 超过 24 小时升级为严重预警。 */
    public static final BigDecimal CRITICAL_HOURS = new BigDecimal("24");

    private SlaCalculator() {
    }

    public record SlaEvaluation(String status, BigDecimal overdueHours, String level) {
    }

    public static SlaEvaluation evaluate(LocalDateTime promisedTime, LocalDateTime actualTime, LocalDateTime now) {
        if (promisedTime == null) {
            return new SlaEvaluation(NA, BigDecimal.ZERO.setScale(2), null);
        }
        LocalDateTime reference = actualTime != null ? actualTime : now;
        if (!reference.isAfter(promisedTime)) {
            return new SlaEvaluation(actualTime != null ? ON_TIME : IN_PROGRESS,
                    BigDecimal.ZERO.setScale(2), null);
        }
        long minutes = Duration.between(promisedTime, reference).toMinutes();
        BigDecimal overdueHours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        String level = overdueHours.compareTo(CRITICAL_HOURS) > 0 ? LEVEL_CRITICAL : LEVEL_WARN;
        return new SlaEvaluation(OVERDUE, overdueHours, level);
    }
}
