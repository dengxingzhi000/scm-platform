package com.cognora.scm.finance.domain.enums;

/**
 * 对账记录状态机。
 *
 * <pre>
 * PENDING(0) ──► RECONCILED(1) ──► CONFIRMED(2)
 *      │              │
 *      └──────────► DIFF(3) ──► CONFIRMED(2)
 *                     │
 *                     └──────► REJECTED(4)
 * </pre>
 *
 * 只有 PENDING 可以进入 RECONCILED / DIFF；RECONCILED 与 DIFF 可以确认或驳回；
 * CONFIRMED 与 REJECTED 是终态，任何流转都会被拒绝。
 */
public enum ReconciliationRecordStatus {

    PENDING(0, "待对账"),
    RECONCILED(1, "已对账"),
    CONFIRMED(2, "已确认"),
    DIFF(3, "有差异"),
    REJECTED(4, "已驳回");

    private final int code;
    private final String label;

    ReconciliationRecordStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ReconciliationRecordStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReconciliationRecordStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public boolean canTransitionTo(ReconciliationRecordStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == RECONCILED || target == DIFF;
            case RECONCILED, DIFF -> target == CONFIRMED || target == REJECTED;
            case CONFIRMED, REJECTED -> false;
        };
    }
}
