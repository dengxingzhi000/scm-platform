package com.scmcloud.logistics.api.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WaybillRequest {

    private Long orderId;
    private Long warehouseId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String carrier;
}
