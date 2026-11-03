package com.scmcloud.supplier.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 供应商表
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sup_supplier")
public class SupSupplier {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("supplier_code")
    private String supplierCode;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("supplier_name_en")
    private String supplierNameEn;

    @TableField("supplier_type")
    private Integer supplierType;

    @TableField("business_type")
    private String businessType;

    @TableField("legal_person")
    private String legalPerson;

    @TableField("registered_capital")
    private BigDecimal registeredCapital;

    @TableField("registration_no")
    private String registrationNo;

    @TableField("tax_no")
    private String taxNo;

    @TableField("establishment_date")
    private LocalDate establishmentDate;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_email")
    private String contactEmail;

    @TableField("contact_address")
    private String contactAddress;

    @TableField("bank_name")
    private String bankName;

    @TableField("bank_account")
    private String bankAccount;

    @TableField("bank_account_name")
    private String bankAccountName;

    @TableField("credit_rating")
    private String creditRating;

    @TableField("quality_score")
    private BigDecimal qualityScore;

    @TableField("delivery_score")
    private BigDecimal deliveryScore;

    @TableField("service_score")
    private BigDecimal serviceScore;

    @TableField("cooperation_start_date")
    private LocalDate cooperationStartDate;

    @TableField("cooperation_status")
    private Integer cooperationStatus;

    @TableField("payment_terms")
    private String paymentTerms;

    @TableField("payment_method")
    private String paymentMethod;

    @TableField("business_license")
    private String businessLicense;

    @TableField("certificates")
    private String certificates;

    @TableField("website")
    private String website;

    @TableField("supplier_level")
    private Integer supplierLevel;

    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("audited_by")
    private String auditedBy;

    @TableField("audited_at")
    private LocalDateTime auditedAt;

    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("blacklisted")
    private Boolean blacklisted;

    @TableField("blacklist_reason")
    private String blacklistReason;

    @TableField("blacklisted_at")
    private LocalDateTime blacklistedAt;

    @TableField("overall_score")
    private BigDecimal overallScore;

    @TableField("last_evaluation_at")
    private LocalDateTime lastEvaluationAt;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("deleted")
    private Boolean deleted;

    @TableField("remark")
    private String remark;


}
