package com.cognora.lms.logistics.domain;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 运单状态与流转规则的唯一事实来源（批次 2）。
 *
 * <p>数据库 {@code tms_waybill.status} 取值 0-6：</p>
 * <ul>
 *   <li>0 待揽件 → 1 已揽件 → 2 运输中 → 3 派送中 → 4 已签收</li>
 *   <li>5 异常件：可恢复运输/派送，或退回</li>
 *   <li>6 退回/取消：终态</li>
 * </ul>
 *
 * <p>已签收(4) 与退回(6) 为不可逆终态；手工 {@code updateStatus} 只接受上表中的显式迁移，
 * 承运商轨迹事件则允许在一定范围内跳跃（见 {@link #isTrackingAdvanceAllowed(int, int)}），
 * 但同样禁止回退与终态改写。</p>
 */
public final class WaybillStatus {

    public static final int CREATED = 0;
    public static final int PICKED_UP = 1;
    public static final int IN_TRANSIT = 2;
    public static final int OUT_FOR_DELIVERY = 3;
    public static final int DELIVERED = 4;
    public static final int EXCEPTION = 5;
    public static final int RETURNED = 6;

    /** 不可逆终态：已签收、退回/取消。 */
    public static final Set<Integer> TERMINAL = Set.of(DELIVERED, RETURNED);

    /** 手工流转白名单；异常件可恢复运输/派送，已签收与退回不可再出发。 */
    private static final Map<Integer, Set<Integer>> ALLOWED = Map.of(
            CREATED, Set.of(PICKED_UP, RETURNED),
            PICKED_UP, Set.of(IN_TRANSIT, EXCEPTION, RETURNED),
            IN_TRANSIT, Set.of(OUT_FOR_DELIVERY, EXCEPTION, RETURNED),
            OUT_FOR_DELIVERY, Set.of(DELIVERED, EXCEPTION, RETURNED),
            DELIVERED, Set.of(),
            EXCEPTION, Set.of(IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, RETURNED),
            RETURNED, Set.of()
    );

    /** 允许取消的来源状态：尚未派送成功（派送中需走退回流程）。 */
    private static final Set<Integer> CANCELLABLE = Set.of(CREATED, PICKED_UP, IN_TRANSIT, EXCEPTION);

    private WaybillStatus() {
    }

    public static boolean isKnown(Integer status) {
        return status != null && status >= CREATED && status <= RETURNED;
    }

    public static boolean isTerminal(Integer status) {
        return status != null && TERMINAL.contains(status);
    }

    public static Set<Integer> allowedTargets(Integer status) {
        if (!isKnown(status)) {
            return Collections.emptySet();
        }
        return ALLOWED.getOrDefault(status, Collections.emptySet());
    }

    public static boolean isCancellable(Integer status) {
        return status != null && CANCELLABLE.contains(status);
    }

    /**
     * 校验手工状态迁移。同状态视为幂等重放直接放行；终态与未知状态一律拒绝。
     *
     * @throws IllegalStatusTransitionException 迁移非法
     */
    public static void assertTransitionAllowed(Integer from, Integer to) {
        if (!isKnown(from)) {
            throw new IllegalStatusTransitionException("未知的运单当前状态: " + from);
        }
        if (!isKnown(to)) {
            throw new IllegalStatusTransitionException("未知的运单目标状态: " + to);
        }
        if (from.equals(to)) {
            return;
        }
        if (TERMINAL.contains(from)) {
            throw new IllegalStatusTransitionException(
                    "运单已处于终态「" + label(from) + "」，不允许流转到「" + label(to) + "」");
        }
        if (!allowedTargets(from).contains(to)) {
            throw new IllegalStatusTransitionException(
                    "非法状态流转: " + label(from) + "(" + from + ") -> " + label(to) + "(" + to + ")");
        }
    }

    /**
     * 承运商轨迹事件驱动的状态推进：只允许向前，允许跳过中间态（承运商常合并推送），
     * 但禁止从终态出发、禁止回退。返回 false 时调用方应保留原状态并记录告警。
     */
    public static boolean isTrackingAdvanceAllowed(Integer from, Integer to) {
        if (!isKnown(from) || !isKnown(to)) {
            return false;
        }
        if (TERMINAL.contains(from)) {
            return false;
        }
        if (to == EXCEPTION || to == RETURNED) {
            return true;
        }
        if (to == PICKED_UP) {
            return from == CREATED;
        }
        if (to == IN_TRANSIT) {
            return from == CREATED || from == PICKED_UP || from == EXCEPTION;
        }
        if (to == OUT_FOR_DELIVERY) {
            return from != OUT_FOR_DELIVERY;
        }
        if (to == DELIVERED) {
            return from == CREATED || from == PICKED_UP || from == IN_TRANSIT
                    || from == OUT_FOR_DELIVERY || from == EXCEPTION;
        }
        return false;
    }

    /**
     * 将承运商轨迹状态码映射为运单状态，未知返回 -1。
     */
    public static int fromTrackingEvent(String trackStatus) {
        if (trackStatus == null) {
            return -1;
        }
        return switch (trackStatus.trim().toUpperCase()) {
            case "PICKED_UP", "PICKED", "COLLECTED", "揽件", "已揽件" -> PICKED_UP;
            case "IN_TRANSIT", "TRANSPORTING", "运输中" -> IN_TRANSIT;
            case "OUT_FOR_DELIVERY", "DELIVERING", "DISPATCHED", "派送中" -> OUT_FOR_DELIVERY;
            case "DELIVERED", "SIGNED", "已签收" -> DELIVERED;
            case "EXCEPTION", "FAILED", "REJECTED", "异常" -> EXCEPTION;
            case "RETURNED", "RETURN", "退回" -> RETURNED;
            default -> -1;
        };
    }

    public static String label(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case CREATED -> "待揽件";
            case PICKED_UP -> "已揽件";
            case IN_TRANSIT -> "运输中";
            case OUT_FOR_DELIVERY -> "派送中";
            case DELIVERED -> "已签收";
            case EXCEPTION -> "异常";
            case RETURNED -> "退回/取消";
            default -> "未知";
        };
    }
}
