package com.frog.finance.api.request;

import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FreightRequest {

    private Long warehouseId;
    private String destinationAddress;
    private BigDecimal weight;
    private BigDecimal volume;
    private String shippingMethod;
}
