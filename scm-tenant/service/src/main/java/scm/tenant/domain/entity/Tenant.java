package scm.tenant.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 租户表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tenant")
public class Tenant implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("tenant_name")
    private String tenantName;

    @TableField("tenant_name_en")
    private String tenantNameEn;

    @TableField("tenant_type")
    private Integer tenantType;

    @TableField("company_name")
    private String companyName;

    @TableField("legal_person")
    private String legalPerson;

    @TableField("registration_no")
    private String registrationNo;

    @TableField("tax_no")
    private String taxNo;

    @TableField("industry")
    private String industry;

    @TableField("company_size")
    private Integer companySize;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_email")
    private String contactEmail;

    @TableField("address")
    private String address;

    @TableField("admin_user_id")
    private String adminUserId;

    @TableField("admin_username")
    private String adminUsername;

    @TableField("admin_email")
    private String adminEmail;

    @TableField("status")
    private Integer status;

    @TableField("trial_start_date")
    private LocalDate trialStartDate;

    @TableField("trial_end_date")
    private LocalDate trialEndDate;

    @TableField("contract_start_date")
    private LocalDate contractStartDate;

    @TableField("contract_end_date")
    private LocalDate contractEndDate;

    @TableField("activated_at")
    private LocalDateTime activatedAt;

    @TableField("suspended_at")
    private LocalDateTime suspendedAt;

    @TableField("logo_url")
    private String logoUrl;

    @TableField("domain")
    private String domain;

    @TableField("theme_config")
    private String themeConfig;

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
