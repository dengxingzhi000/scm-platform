package com.cognora.scm.finance.domain.enums;

/**
 * 会计期间（账期）状态。
 *
 * <p>合法流转（唯一合法回退路径是「已结账 → 已重开」）：</p>
 * <pre>
 * OPEN ──close──▶ CLOSED ──reopen──▶ REOPENED ──close──▶ CLOSED
 * </pre>
 *
 * <p>重开只允许从 {@link #CLOSED} 发起，重开后该账期恢复可写；再次关账回到
 * {@link #CLOSED}。其余流转（例如未结账直接重开、重开态重复重开）一律拒绝。
 * 重开是账期唯一的合法回退路径，关账写入守卫只拦截 {@link #CLOSED}。</p>
 */
public enum AccountingPeriodStatus {

    OPEN(0, "未结账"),
    CLOSED(1, "已结账"),
    REOPENED(2, "已重开");

    private final int code;
    private final String description;

    AccountingPeriodStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 唯一合法流转：未结账 → 已结账、已结账 → 已重开、已重开 → 已结账。
     */
    public boolean canTransitionTo(AccountingPeriodStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case OPEN -> target == CLOSED;
            case CLOSED -> target == REOPENED;
            case REOPENED -> target == CLOSED;
        };
    }

    public static AccountingPeriodStatus fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("账期状态不能为空");
        }
        for (AccountingPeriodStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的账期状态: " + code);
    }
}
