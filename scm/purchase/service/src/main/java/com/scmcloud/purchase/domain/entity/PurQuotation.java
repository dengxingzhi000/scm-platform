package com.scmcloud.purchase.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 渚涘簲鍟嗘姤浠峰崟锟?
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_quotation")
public class PurQuotation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    private String quotationNo;

    private String rfqId;

    private String rfqNo;

    private String supplierId;

    private String supplierName;

    private String supplierContact;

    private String supplierPhone;

    private String supplierEmail;

    private BigDecimal totalAmount;

    private BigDecimal taxRate;

    private String paymentTerms;

    private String paymentMethod;

    private String deliveryTerms;

    private String deliveryPeriod;

    private String warrantyPeriod;

    private String warrantyTerms;

    private LocalDate validFrom;

    private LocalDate validUntil;
    private Integer status;

    private Boolean isSelected;

    private String selectedBy;

    private String selectedByName;

    private LocalDateTime selectedAt;

    private String attachments;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    private Boolean deleted;

    private String remark;


}
