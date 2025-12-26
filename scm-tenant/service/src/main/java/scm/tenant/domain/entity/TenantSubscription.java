package scm.tenant.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 租户订阅表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tenant_subscription")
@Schema(description = "租户订阅表")
public class TenantSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("package_id")
    private String packageId;

    @Schema(description = "订阅类型:1-月付,2-年付,3-一次性")
    @TableField("subscription_type")
    private Integer subscriptionType;

    @Schema(description = "状态:0-待支付,1-生效中,2-已过期,3-已取消")
    @TableField("status")
    private Integer status;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("auto_renew")
    private Boolean autoRenew;

    @TableField("original_price")
    private BigDecimal originalPrice;

    @TableField("actual_price")
    private BigDecimal actualPrice;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @Schema(description = "支付状态:0-未支付,1-部分支付,2-已支付")
    @TableField("payment_status")
    private Integer paymentStatus;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("payment_no")
    private String paymentNo;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("remark")
    private String remark;


}
