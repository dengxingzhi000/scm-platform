package com.scmcloud.logistics.api.dto;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WaybillVO {

    private Long id;
    private String waybillNo;
    private Long orderId;
    private Long warehouseId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String carrier;
    private String trackingNo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
