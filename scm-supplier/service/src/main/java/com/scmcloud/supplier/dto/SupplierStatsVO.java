package com.scmcloud.supplier.dto;

import lombok.Data;

import java.util.Map;

/**
 * 供应商管理概览统计。
 */
@Data
public class SupplierStatsVO {

    private long total;
    private long enabled;
    private long cooperating;
    private long pendingAudit;
    private long blacklisted;
    private long expiringCertificates;
    private Map<Integer, Long> byCooperationStatus;
    private Map<Integer, Long> byAuditStatus;
}
