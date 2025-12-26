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
 * 采购计划明细表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_plan_item")
@Schema(description = "采购计划明细表")
public class PurPlanItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    private String planId;

    private String planNo;

    private String materialCode;

    private String materialName;

    private String materialCategory;

    private BigDecimal plannedQuantity;

    private String unit;

    private BigDecimal purchasedQuantity;

    private BigDecimal budgetPrice;

    private BigDecimal budgetAmount;

    private LocalDate expectedDelivery;

    private LocalDateTime createTime;

    private String remark;


}
