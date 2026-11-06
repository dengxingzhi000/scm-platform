package com.cognora.scm.finance.domain.enums;

/**
 * 会计期间（账期）状态。
 *
 * <p>本批次只实现「开账期 → 关账期」单向流转；重开账期涉及跨期调整分录与
 * 已结账报表重算，属于未定业务口径，暂不提供。</p>
 */
public enum AccountingPeriodStatus {

    OPEN(0, "未结账"),
    CLOSED(1, "已结账");

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
     * 唯一合法流转：未结账 → 已结账。
     */
    public boolean canTransitionTo(AccountingPeriodStatus target) {
        return this == OPEN && target == CLOSED;
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
