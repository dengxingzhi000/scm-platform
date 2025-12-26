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
 * 采购订单明细表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_order_item")
@Schema(description = "采购订单明细表")
public class PurOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    private String orderId;

    private String orderNo;

    private String materialCode;

    private String materialName;

    private String materialSpec;

    private String materialCategory;

    private String skuId;

    private String skuCode;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal receivedQuantity;

    private BigDecimal qualifiedQuantity;

    private BigDecimal unitPrice;

    private BigDecimal taxRate;

    private BigDecimal discountRate;

    private BigDecimal subtotal;

    private LocalDate deliveryDate;

    private String qualityRequirement;

    private String itemRemark;

    private LocalDateTime createTime;


}
