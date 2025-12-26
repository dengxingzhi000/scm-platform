package scm.purchase.domain.entity;

import java.math.BigDecimal;
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
 * 采购申请单表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_request")
@Schema(description = "采购申请单表")
public class PurRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    private String requestNo;

    @Schema(description = "申请类型:1-常规采购,2-紧急采购,3-补货采购,4-工程采购")
    private Integer requestType;

    @Schema(description = "优先级:1-紧急,2-普通,3-低")
    private Integer priority;

    private String deptId;

    private String deptName;

    private String requesterId;

    private String requesterName;

    private String requesterPhone;

    private LocalDateTime expectedDelivery;

    private String purpose;

    private BigDecimal budgetAmount;

    @Schema(description = "状态:0-草稿,1-待审批,2-已审批,3-已驳回,4-已转采购单,5-已关闭")
    private Integer status;

    private String approvalFlowId;

    private String currentApproverId;

    private String currentApproverName;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private String rejectReason;

    private Boolean converted;

    private String purchaseOrderId;

    private String purchaseOrderNo;

    private String attachments;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    private Boolean deleted;

    private String remark;


}
