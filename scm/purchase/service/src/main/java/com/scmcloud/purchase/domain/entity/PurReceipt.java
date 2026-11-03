package com.scmcloud.purchase.domain.entity;

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
public class PurReceipt implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    private String receiptNo;

    private String orderId;

    private String orderNo;

    private String supplierId;

    private String supplierName;

    private String warehouseId;

    private String warehouseName;
    private Integer receiptType;
    private Integer status;

    private String receiverId;

    private String receiverName;

    private LocalDateTime receivedAt;

    private String qualityInspectorId;

    private String qualityInspectorName;

    private LocalDateTime qualityInspectedAt;
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
