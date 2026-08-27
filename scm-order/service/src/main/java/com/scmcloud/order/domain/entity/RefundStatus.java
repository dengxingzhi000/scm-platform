package com.scmcloud.order.domain.entity;

import lombok.Getter;
import java.util.UUID;

/**
 * 退款单状态枚举。
 *
 * <p>对应 {@code ord_refund.status} 列，DB CHECK 约束为 {@code status IN (0,1,2,3)}。</p>
 *
 * <pre>
 * PENDING ──→ APPROVED ──→ COMPLETED
 *     │           │
 *     └──→ REJECTED
 * </pre>
 *
 * @author SCM Platform Team
 */
@Getter
public enum RefundStatus {

    PENDING(0, "待审核"),
    APPROVED(1, "已同意"),
    REJECTED(2, "已拒绝"),
    COMPLETED(3, "已完成");

    private final int code;
    private final String description;

    RefundStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static RefundStatus fromCode(int code) {
        for (RefundStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown refund status code: " + code);
    }

    /**
     * 是否可被审核（仅待审核状态可被通过或拒绝）。
     */
    public boolean isReviewable() {
        return this == PENDING;
    }

    /**
     * 是否处于终态。
     */
    public boolean isTerminal() {
        return this == REJECTED || this == COMPLETED;
    }
}
