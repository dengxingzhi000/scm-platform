package scm.finance.domain.entity;

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
 * 发票表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("invoice")
@Schema(description = "发票表")
public class Invoice implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("invoice_no")
    private String invoiceNo;

    @Schema(description = "发票类型:1-增值税普通发票,2-增值税专用发票,3-电子发票")
    @TableField("invoice_type")
    private Integer invoiceType;

    @Schema(description = "发票方向:1-销项发票,2-进项发票")
    @TableField("invoice_kind")
    private Integer invoiceKind;

    @TableField("party_type")
    private String partyType;

    @TableField("party_id")
    private String partyId;

    @TableField("party_name")
    private String partyName;

    @TableField("party_tax_no")
    private String partyTaxNo;

    @TableField("invoice_code")
    private String invoiceCode;

    @TableField("invoice_number")
    private String invoiceNumber;

    @TableField("invoice_date")
    private LocalDate invoiceDate;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("tax_amount")
    private BigDecimal taxAmount;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("tax_rate")
    private BigDecimal taxRate;

    @TableField("related_orders")
    private String relatedOrders;

    @TableField("settlement_id")
    private String settlementId;

    @TableField("issuer_name")
    private String issuerName;

    @TableField("issue_date")
    private LocalDate issueDate;

    @Schema(description = "状态:0-草稿,1-已开具,2-已邮寄,3-已作废,4-已红冲")
    @TableField("status")
    private Integer status;

    @TableField("invoice_file_url")
    private String invoiceFileUrl;

    @TableField("invoice_pdf_url")
    private String invoicePdfUrl;

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
