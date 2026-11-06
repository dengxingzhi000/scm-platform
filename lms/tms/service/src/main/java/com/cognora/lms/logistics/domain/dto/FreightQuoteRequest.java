package com.cognora.lms.logistics.domain.dto;

import java.math.BigDecimal;

/**
 * 运费试算请求（批次 2）。
 */
public record FreightQuoteRequest(
        String carrierId,
        String regionCode,
        BigDecimal weight,
        BigDecimal volume
) {
}
