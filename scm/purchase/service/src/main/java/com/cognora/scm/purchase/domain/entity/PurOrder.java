package com.cognora.scm.purchase.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 采购订单表（分区）
 * </p>
 *
 * <p>
 * 该实体只映射采购单查询需要的列。写入仍由采购申请/询价流程完成，读接口供
 * cognora-aura-web 的采购单列表与详情使用。
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_order")
public class PurOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("order_no")
    private String orderNo;

    @TableField("request_id")
    private String requestId;

    @TableField("request_no")
    private String requestNo;

    @TableField("supplier_id")
    private String supplierId;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("supplier_contact")
    private String supplierContact;

    @TableField("order_type")
    private Integer orderType;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("tax_amount")
    private BigDecimal taxAmount;

    @TableField("freight_amount")
    private BigDecimal freightAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("payable_amount")
    private BigDecimal payableAmount;

    @TableField("status")
    private Integer status;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("warehouse_name")
    private String warehouseName;

    @TableField("warehouse_address")
    private String warehouseAddress;

    @TableField("order_date")
    private LocalDate orderDate;

    @TableField("expected_delivery")
    private LocalDateTime expectedDelivery;

    @TableField("actual_delivery")
    private LocalDateTime actualDelivery;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;

    @TableField("buyer_id")
    private String buyerId;

    @TableField("buyer_name")
    private String buyerName;

    @TableField("approved_by")
    private String approvedBy;

    @TableField("approved_by_name")
    private String approvedByName;

    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("deleted")
    private Boolean deleted;

    @TableField("remark")
    private String remark;
}
