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
 * 结算明细表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("settlement_item")
@Schema(description = "结算明细表")
public class SettlementItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("settlement_id")
    private String settlementId;

    @TableField("settlement_no")
    private String settlementNo;

    @Schema(description = "单据类型:ORDER-订单,PURCHASE-采购单,WAYBILL-运单")
    @TableField("document_type")
    private String documentType;

    @TableField("document_id")
    private String documentId;

    @TableField("document_no")
    private String documentNo;

    @TableField("document_date")
    private LocalDate documentDate;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("discount")
    private BigDecimal discount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("is_settled")
    private Boolean isSettled;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("remark")
    private String remark;

}
