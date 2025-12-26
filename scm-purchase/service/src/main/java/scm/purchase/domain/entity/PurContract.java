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
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "采购合同表")
public class PurContract implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    @Schema(description = "合同ID")
    private String id;


    @TableField("tenant_id")
    @Schema(description = "租户ID")
    private String tenantId;

    @TableField("contract_no")
    @Schema(description = "合同编号")
    private String contractNo;


    @TableField("contract_name")
    @Schema(description = "合同名称")
    private String contractName;

    @TableField("contract_type")
    @Schema(description = "合同类型:1-框架协议,2-采购合同,3-补充协议")
    private Integer contractType;


    @TableField("supplier_id")
    @Schema(description = "供应商ID")
    private String supplierId;

    @TableField("supplier_name")
    @Schema(description = "供应商名称")
    private String supplierName;

    @TableField("contract_amount")
    @Schema(description = "合同金额")
    private BigDecimal contractAmount;


    @TableField("start_date")
    @Schema(description = "开始日期")
    private LocalDate startDate;

    @TableField("end_date")
    @Schema(description = "结束日期")
    private LocalDate endDate;

    @TableField("payment_terms")
    @Schema(description = "付款条款")
    private String paymentTerms;

    @TableField("delivery_terms")
    @Schema(description = "交货条款")
    private String deliveryTerms;

    @TableField("quality_terms")
    @Schema(description = "质量条款")
    private String qualityTerms;

    @TableField("penalty_terms")
    @Schema(description = "违约条款")
    private String penaltyTerms;

    @TableField("status")
    @Schema(description = "状态:0-草稿,1-待签署,2-执行中,3-已完成,4-已终止")
    private Integer status;


    @TableField("signed_by")
    @Schema(description = "签署人ID")
    private String signedBy;

    @TableField("signed_by_name")
    @Schema(description = "签署人姓名")
    private String signedByName;

    @TableField("signed_at")
    @Schema(description = "签署时间")
    private LocalDateTime signedAt;

    @TableField("party_a_representative")
    @Schema(description = "甲方代表")
    private String partyARepresentative;

    @TableField("party_a_contact")
    @Schema(description = "甲方联系方式")
    private String partyAContact;

    @TableField("party_b_representative")
    @Schema(description = "乙方代表")
    private String partyBRepresentative;

    @TableField("party_b_contact")
    @Schema(description = "乙方联系方式")
    private String partyBContact;

    @TableField("contract_files")
    @Schema(description = "合同文件")
    private String contractFiles;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("create_by")
    @Schema(description = "创建人")
    private String createBy;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField("update_by")
    @Schema(description = "更新人")
    private String updateBy;

    @TableField("deleted")
    @Schema(description = "是否删除")
    private Boolean deleted;

    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

}
