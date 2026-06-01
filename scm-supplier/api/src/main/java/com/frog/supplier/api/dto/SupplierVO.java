package com.frog.supplier.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 供应商信息
 */
@Data
@Accessors(chain = true)
public class SupplierVO {

    private Long id;
    private String name;
    private String code;
    private String contactPerson;
    private String contactPhone;
    private String email;
    private String address;
    private Integer status;
    private Integer level;
}
