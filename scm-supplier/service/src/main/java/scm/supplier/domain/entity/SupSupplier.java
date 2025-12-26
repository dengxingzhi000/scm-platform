package scm.supplier.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "供应商表")
public class SupSupplier implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField("id")
    private String id;

    @TableId(value = "id", type = IdType.UUID)
    private String id;

    @TableField("supplier_code")
    private String supplierCode;

    @TableField("supplier_code")
    private String supplierCode;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("supplier_name_en")
    private String supplierNameEn;

    @Schema(description = "类型:1-生产商,2-贸易商,3-代理商,4-其他")
    @TableField("supplier_type")
    private Integer supplierType;

    @Schema(description = "类型:1-生产商,2-贸易商,3-代理商,4-其他")
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

    @Schema(description = "信用评级:A+,A,B+,B,C,D")
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

    @Schema(description = "合作状态:0-潜在,1-合作中,2-暂停,3-终止")
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
