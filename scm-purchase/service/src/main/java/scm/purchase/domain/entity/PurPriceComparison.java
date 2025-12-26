package scm.purchase.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 比价分析表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_price_comparison")
@Schema(description = "比价分析表")
public class PurPriceComparison implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    private String comparisonNo;

    private String rfqId;

    private String rfqNo;

    private String comparisonDimensions;

    private String recommendedSupplierId;

    private String recommendedSupplierName;

    private String recommendationReason;

    @Schema(description = "状态:0-比价中,1-已完成,2-已审批")
    private Integer status;

    private String approvedBy;

    private String approvedByName;

    private LocalDateTime approvedAt;

    private String analystId;

    private String analystName;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    private Boolean deleted;

    private String remark;


}
