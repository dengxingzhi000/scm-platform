package com.cognora.scm.purchase.api.request;

import com.cognora.scm.purchase.api.dto.PurchaseItemDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class PurchaseRequestDTO {

    private Long supplierId;
    private Long warehouseId;
    private Long applicantId;
    private String remark;
    private List<PurchaseItemDTO> items;
}
