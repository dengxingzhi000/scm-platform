package com.scmcloud.warehouse.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class NearExpiryProductVO {
    private UUID tenantId;
    private String skuId;
    private String skuName;
    private String batchNo;
    private Integer quantity;
    private LocalDate expiryDate;
    private int daysUntilExpiry;
    private String alertLevel; // CRITICAL, WARNING, INFO
}
