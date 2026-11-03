package com.scmcloud.supplier.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商资质证照，valid_to 用于到期预警。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sup_supplier_certificate")
public class SupSupplierCertificate {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("supplier_id")
    private String supplierId;

    @TableField("certificate_type")
    private String certificateType;

    @TableField("certificate_name")
    private String certificateName;

    @TableField("certificate_no")
    private String certificateNo;

    @TableField("issuing_authority")
    private String issuingAuthority;

    @TableField("valid_from")
    private LocalDate validFrom;

    @TableField("valid_to")
    private LocalDate validTo;

    @TableField("file_url")
    private String fileUrl;

    @TableField("status")
    private Integer status;

    @TableField("verified_by")
    private String verifiedBy;

    @TableField("verified_at")
    private LocalDateTime verifiedAt;

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

    /** 距离到期天数，仅查询时回填。 */
    @TableField(exist = false)
    private Integer daysToExpiry;

    /** 供应商名称，仅跨供应商到期查询时回填。 */
    @TableField(exist = false)
    private String supplierName;
}
