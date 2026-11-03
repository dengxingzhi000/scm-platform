package com.scmcloud.purchase.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 供应商报价明细表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_quotation_item")
public class PurQuotationItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    private String quotationId;

    private String quotationNo;

    private String rfqItemId;

    private String materialCode;

    private String materialName;

    private String materialSpec;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal unitPrice;

    private BigDecimal taxRate;

    private BigDecimal subtotal;

    private Integer deliveryDays;

    private String brand;

    private String origin;

    private LocalDateTime createTime;

    private String remark;


}
