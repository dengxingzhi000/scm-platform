package com.cognora.scm.finance.domain.enums;

/**
 * 对账差异状态机。
 *
 * <p>与 {@code reconciliation_record.status}（0 草稿 / 1 对账中 / 2 已确认 /
 * 3 有差异）不是同一套编码，差异表使用独立的 0-3 数字状态。</p>
 *
 * <pre>
 * DISCOVERED(0) ──┬─→ CONFIRMED(1) ──→ WRITTEN_OFF(2)
 *                 └─→ REJECTED(3)
 * </pre>
 */
public enum ReconciliationDiffStatus {

    /** 差异已发现，待处理。 */
    DISCOVERED(0, "待处理"),
    /** 差异已确认，待核销。 */
    CONFIRMED(1, "已确认"),
    /** 差异已核销豁免（终态）。 */
    WRITTEN_OFF(2, "已核销"),
    /** 差异被驳回（终态）。 */
    REJECTED(3, "已驳回");

    private final int code;
    private final String description;

    ReconciliationDiffStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(ReconciliationDiffStatus target) {
        return switch (this) {
            case DISCOVERED -> target == CONFIRMED || target == REJECTED;
            case CONFIRMED -> target == WRITTEN_OFF;
            case WRITTEN_OFF, REJECTED -> false;
        };
    }

    public static ReconciliationDiffStatus fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("差异状态不能为空");
        }
        for (ReconciliationDiffStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的差异状态: " + code);
    }
}
