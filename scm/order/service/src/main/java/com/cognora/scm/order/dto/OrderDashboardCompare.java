package com.cognora.scm.order.dto;

/**
 * 订单看板对比基准。
 */
public enum OrderDashboardCompare {

    /** 不对比。 */
    NONE,
    /** 环比：与紧邻的等长周期对比。 */
    PREVIOUS_PERIOD,
    /** 同比：与去年同期对比。 */
    YEAR_OVER_YEAR
}
