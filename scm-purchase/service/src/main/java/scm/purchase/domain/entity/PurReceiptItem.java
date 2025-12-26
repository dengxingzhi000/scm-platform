package scm.purchase.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 采购入库明细表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_receipt_item")
@Schema(description = "采购入库明细表")
public class PurReceiptItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    private String receiptId;

    private String receiptNo;

    private String orderItemId;

    private String materialCode;

    private String materialName;

    private BigDecimal expectedQuantity;

    private BigDecimal actualQuantity;

    private BigDecimal qualifiedQuantity;

    private BigDecimal unqualifiedQuantity;

    private String unit;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expiryDate;

    @Schema(description = "质检结果:1-合格,2-不合格,3-待定")
    private Integer qualityResult;

    private String locationCode;

    private LocalDateTime createTime;

    private String remark;


}
