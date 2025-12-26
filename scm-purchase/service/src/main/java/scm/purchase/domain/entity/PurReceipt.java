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
 * 采购入库单表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_receipt")
@Schema(description = "采购入库单表")
public class PurReceipt implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    private String receiptNo;

    private String orderId;

    private String orderNo;

    private String supplierId;

    private String supplierName;

    private String warehouseId;

    private String warehouseName;

    @Schema(description = "收货类型:1-正常收货,2-退货,3-换货")
    private Integer receiptType;

    @Schema(description = "状态:0-待收货,1-已收货,2-已质检,3-已入库")
    private Integer status;

    private String receiverId;

    private String receiverName;

    private LocalDateTime receivedAt;

    private String qualityInspectorId;

    private String qualityInspectorName;

    private LocalDateTime qualityInspectedAt;

    @Schema(description = "质检结果:1-合格,2-不合格,3-部分合格")
    private Integer qualityResult;

    private String qualityRemark;

    private Boolean shelved;

    private String shelvedBy;

    private String shelvedByName;

    private LocalDateTime shelvedAt;

    private String attachments;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    private Boolean deleted;

    private String remark;


}
