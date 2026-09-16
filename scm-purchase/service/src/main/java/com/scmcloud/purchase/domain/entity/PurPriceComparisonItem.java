package com.scmcloud.purchase.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 姣斾环鏄庣粏锟?
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_price_comparison_item")
public class PurPriceComparisonItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    private String comparisonId;

    private String quotationId;

    private String supplierId;

    private String supplierName;

    private String scores;

    private BigDecimal totalScore;

    private Integer rank;

    private String advantages;

    private String disadvantages;

    private LocalDateTime createTime;

    private String remark;


}
