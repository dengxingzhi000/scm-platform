package com.scmcloud.warehouse.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WarehouseVO {

    private Long id;
    private String name;
    private String code;
    private String address;
    private String contactPerson;
    private String contactPhone;
    private Integer status;
}
