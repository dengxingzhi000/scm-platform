package com.scmcloud.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商主数据新增/修改请求，仅暴露可维护字段。
 * 准入状态、黑名单、评分等由服务端流程维护，不接受客户端直接写入。
 */
@Data
public class SupplierUpsertRequest {

    @Size(max = 64, message = "供应商编码长度不能超过 64")
    private String supplierCode;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 256, message = "供应商名称长度不能超过 256")
    private String supplierName;

    @Size(max = 256, message = "英文名称长度不能超过 256")
    private String supplierNameEn;

    private Integer supplierType;

    @Size(max = 64, message = "业务类型长度不能超过 64")
    private String businessType;

    @Size(max = 128, message = "法人姓名长度不能超过 128")
    private String legalPerson;

    private BigDecimal registeredCapital;

    @Size(max = 64, message = "统一社会信用代码长度不能超过 64")
    private String registrationNo;

    @Size(max = 64, message = "税号长度不能超过 64")
    private String taxNo;

    private LocalDate establishmentDate;

    @Size(max = 128, message = "联系人长度不能超过 128")
    private String contactName;

    @Size(max = 32, message = "联系电话长度不能超过 32")
    private String contactPhone;

    @Email(message = "联系邮箱格式不正确")
    @Size(max = 128, message = "联系邮箱长度不能超过 128")
    private String contactEmail;

    @Size(max = 512, message = "联系地址长度不能超过 512")
    private String contactAddress;

    @Size(max = 256, message = "开户行长度不能超过 256")
    private String bankName;

    @Size(max = 64, message = "银行账号长度不能超过 64")
    private String bankAccount;

    @Size(max = 128, message = "账户名称长度不能超过 128")
    private String bankAccountName;

    @Size(max = 16, message = "信用等级长度不能超过 16")
    private String creditRating;

    private LocalDate cooperationStartDate;
    private String paymentTerms;
    private String paymentMethod;
    private String businessLicense;
    private String certificates;
    private String website;
    private Integer sortOrder;
    private String remark;
}
