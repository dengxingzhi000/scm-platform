package com.scmcloud.order.domain.entity;

import lombok.Getter;
import java.util.UUID;

/**
 * 支付单状态枚举。
 *
 * <p>对应 {@code ord_payment.status} 列，DB CHECK 约束为 {@code status IN (0,1,2,3,4,5,6)}，
 * 名称与 {@code sys_status_dict} 中 "PAYMENT" 类型注册的字典值一致，
 * 可直接传给 {@code StatusValidator.validateTransition("PAYMENT", ...)}。</p>
 *
 * <pre>
 * PENDING ──→ PROCESSING ──→ SUCCESS ──→ REFUNDING ──→ REFUNDED
 *                  │            │              │
 *                  └──→ FAILED  └──→ CANCELLED  └──→ REFUNDED
 * </pre>
 *
 * @author SCM Platform Team
 */
@Getter
public enum PaymentStatus {

    PENDING(0, "待支付"),
    PROCESSING(1, "处理中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败"),
    REFUNDING(4, "退款中"),
    REFUNDED(5, "已退款"),
    CANCELLED(6, "已取消");

    private final int code;
    private final String description;

    PaymentStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PaymentStatus fromCode(int code) {
        for (PaymentStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown payment status code: " + code);
    }

    /**
     * 是否已支付成功（含退款中 / 已退款，因为这些状态的入账已发生）。
     */
    public boolean isPaid() {
        return this == SUCCESS || this == REFUNDING || this == REFUNDED;
    }

    /**
     * 是否处于终态。
     */
    public boolean isTerminal() {
        return this == FAILED || this == REFUNDED || this == CANCELLED;
    }
}
