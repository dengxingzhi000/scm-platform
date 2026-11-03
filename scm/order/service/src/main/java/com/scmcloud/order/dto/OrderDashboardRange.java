package com.scmcloud.order.dto;

import lombok.Getter;

/**
 * 订单看板时间范围。
 */
@Getter
public enum OrderDashboardRange {

    TODAY(1),
    LAST_7_DAYS(7),
    LAST_30_DAYS(30);

    private final int days;

    OrderDashboardRange(int days) {
        this.days = days;
    }
}
