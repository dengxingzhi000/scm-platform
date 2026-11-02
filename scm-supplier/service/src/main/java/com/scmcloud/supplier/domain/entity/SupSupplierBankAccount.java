package com.scmcloud.supplier.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 供应商银行账户（多账户，一个默认账户）。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sup_supplier_bank_account")
public class SupSupplierBankAccount {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("supplier_id")
    private String supplierId;

    @TableField("bank_name")
    private String bankName;

    @TableField("bank_branch")
    private String bankBranch;

    @TableField("bank_account")
    private String bankAccount;

    @TableField("account_name")
    private String accountName;

    @TableField("currency")
    private String currency;

    @TableField("swift_code")
    private String swiftCode;

    @TableField("is_default")
    private Boolean isDefault;

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
