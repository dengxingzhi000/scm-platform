package scm.purchase.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 采购计划表（MRP？
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_plan")
public class PurPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    private String planNo;

    private String planName;
    private Integer planType;

    private String planPeriod;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal totalBudget;
    private Integer status;

    private String approvedBy;

    private String approvedByName;

    private LocalDateTime approvedAt;

    private BigDecimal executionRate;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    private Boolean deleted;

    private String remark;


}
