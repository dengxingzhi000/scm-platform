package com.frog.warehouse.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LocationVO {

    private Long id;
    private Long warehouseId;
    private String locationCode;
    private String zone;
    private Integer capacity;
    private Integer usedCapacity;
    private Integer status;
}
