package com.cognora.scm.finance.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 税率规则（{@code fin_tax_rate}）。
 *
 * <p>租户维度，按品类 / 地区 / 生效区间匹配；{@code category} 与
 * {@code region} 为 {@code null} 表示该维度不限。{@code isDefault=true}
 * 的行是租户默认税率，不参与「品类 × 地区」规则匹配。</p>
 *
 * <p>税率以 0..1 的小数表示（13% 存为 {@code 0.1300}），与发票
 * {@code tax_rate} 口径一致。</p>
 */
@Data
@TableName("fin_tax_rate")
public class TaxRate {

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    /** 品类编码；为空表示不限品类。 */
    private String category;

    /** 地区编码；为空表示不限地区。 */
    private String region;

    /** 税率 0..1，四位小数。 */
    private BigDecimal rate;

    /** 生效开始日期（含）。 */
    private LocalDate effectiveFrom;

    /** 生效结束日期（含）；为空表示长期有效。 */
    private LocalDate effectiveTo;

    /** 是否为租户默认税率；默认税率不参与规则匹配。 */
    private Boolean isDefault;

    /** 是否启用；停用规则不参与匹配。 */
    private Boolean enabled;

    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    private Boolean deleted;
}
