package scm.purchase.domain.entity;

import java.io.Serial;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * 采购合同表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_contract")
public class PurContract implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;


    @TableField("tenant_id")
    private String tenantId;

    @TableField("contract_no")
    private String contractNo;


    @TableField("contract_name")
    private String contractName;

    @TableField("contract_type")
    private Integer contractType;


    @TableField("supplier_id")
    private String supplierId;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("contract_amount")
    private BigDecimal contractAmount;


    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("payment_terms")
    private String paymentTerms;

    @TableField("delivery_terms")
    private String deliveryTerms;

    @TableField("quality_terms")
    private String qualityTerms;

    @TableField("penalty_terms")
    private String penaltyTerms;

    @TableField("status")
    private Integer status;


    @TableField("signed_by")
    private String signedBy;

    @TableField("signed_by_name")
    private String signedByName;

    @TableField("signed_at")
    private LocalDateTime signedAt;

    @TableField("party_a_representative")
    private String partyARepresentative;

    @TableField("party_a_contact")
    private String partyAContact;

    @TableField("party_b_representative")
    private String partyBRepresentative;

    @TableField("party_b_contact")
    private String partyBContact;

    @TableField("contract_files")
    private String contractFiles;

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
