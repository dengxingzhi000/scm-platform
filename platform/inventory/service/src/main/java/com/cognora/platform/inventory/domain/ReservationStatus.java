package com.cognora.platform.inventory.domain;

import java.util.Set;

/**
 * 预占记录状态机。
 *
 * <p>只有 RESERVED 可以流转到其余三种终态，终态之间不可互转：
 * <pre>
 * RESERVED → CONFIRMED
 * RESERVED → RELEASED
 * RESERVED → EXPIRED
 * </pre>
 *
 * @author SCM Platform Team
 * @since 2026-11-04
 */
public enum ReservationStatus {

    RESERVED,
    CONFIRMED,
    RELEASED,
    EXPIRED;

    private static final Set<ReservationStatus> TERMINAL = Set.of(CONFIRMED, RELEASED, EXPIRED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean canTransitionTo(ReservationStatus target) {
        return target != null && this == RESERVED && target.isTerminal();
    }
}
