package com.cognora.scm.finance.domain.enums;

/**
 * 坏账核销单据状态机。
 *
 * <pre>
 * PENDING(0) ──► APPROVED(1) ──► WRITTEN_OFF(2)
 *      │
 *      └──────────────► REJECTED(3)
 * </pre>
 *
 * 只有「申请中」可以审批通过或驳回；核销生效必须来自「已批准」，
 * 因此不存在跳过审批直接核销的路径。{@code WRITTEN_OFF} 与 {@code REJECTED}
 * 是终态，任何流转都会被拒绝。
 */
public enum BadDebtWriteOffStatus {

    PENDING(0, "申请中"),
    APPROVED(1, "已批准"),
    WRITTEN_OFF(2, "已核销"),
    REJECTED(3, "已驳回");

    private final int code;
    private final String label;

    BadDebtWriteOffStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static BadDebtWriteOffStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (BadDebtWriteOffStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public boolean canTransitionTo(BadDebtWriteOffStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == APPROVED || target == REJECTED;
            case APPROVED -> target == WRITTEN_OFF;
            case WRITTEN_OFF, REJECTED -> false;
        };
    }

    /**
     * 占用中的状态：同一单据在这些状态下不允许再次提交核销申请。
     */
    public boolean isActive() {
        return this == PENDING || this == APPROVED || this == WRITTEN_OFF;
    }
}
