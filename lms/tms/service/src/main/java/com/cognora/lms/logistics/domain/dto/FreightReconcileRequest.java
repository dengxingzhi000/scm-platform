package com.cognora.lms.logistics.domain.dto;

import java.math.BigDecimal;

/**
 * 运费对账请求（批次 2）。
 */
public record FreightReconcileRequest(
        String waybillId,
        String regionCode,
        BigDecimal billedAmount,
        String batchNo,
        String operator
) {
}
