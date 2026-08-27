package com.scmcloud.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订单明细请求（创建订单时使用）。
 *
 * @author SCM Platform Team
 */
@Data
public class OrderItemRequest {

    /** SKU ID */
    @NotBlank(message = "SKU ID 不能为空")
    private String skuId;

    /** SPU ID */
    private String spuId;

    /** SKU 编码 */
    private String skuCode;

    /** SKU 名称 */
    @NotBlank(message = "SKU 名称不能为空")
    private String skuName;

    /** SPU 名称 */
    private String spuName;

    /** 销售价（元，保留两位小数） */
    @NotNull(message = "销售价不能为空")
    private BigDecimal sellingPrice;

    /** 购买数量 */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须 ≥ 1")
    private Integer quantity;

    /** 仓库 ID */
    @NotBlank(message = "仓库 ID 不能为空")
    private String warehouseId;

    /** 备注 */
    private String remark;
}
