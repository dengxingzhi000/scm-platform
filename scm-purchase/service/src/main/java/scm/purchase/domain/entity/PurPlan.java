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
 * 采购计划表（MRP）
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_plan")
@Schema(description = "采购计划表（MRP）")
public class PurPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    private String planNo;

    private String planName;

    @Schema(description = "计划类型:1-月度计划,2-季度计划,3-年度计划")
    private Integer planType;

    private String planPeriod;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal totalBudget;

    @Schema(description = "状态:0-编制中,1-待审批,2-执行中,3-已完成")
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
