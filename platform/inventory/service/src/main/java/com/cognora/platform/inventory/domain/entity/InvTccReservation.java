package com.cognora.platform.inventory.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存 TCC 预留记录
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
@TableName("inv_tcc_reservation")
public class InvTccReservation {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务键（订单号等），用于幂等性
     */
    private String businessKey;

    /**
     * SKU ID（旧数字 ID，兼容 scm-order，保留结转期双写）
     */
    private Long skuId;

    /**
     * SKU UUID，与 inv_inventory.sku_id 类型对齐
     */
    private String skuUuid;

    /**
     * 精确台账行：inv_inventory.id
     */
    private String inventoryId;

    /**
     * 仓库 ID，多仓场景下与 skuUuid 一起定位台账行
     */
    private String warehouseId;

    /**
     * 预留数量
     */
    private Integer quantity;

    /**
     * Seata 全局事务 ID
     */
    private String xid;

    /**
     * Seata 分支事务 ID
     */
    private Long branchId;

    /**
     * 状态 TRYING, CONFIRMED, CANCELLED
     */
    private String status;

    /**
     * Try 阶段时间
     */
    private LocalDateTime tryTime;

    /**
     * Confirm 阶段时间
     */
    private LocalDateTime confirmTime;

    /**
     * Cancel 阶段时间
     */
    private LocalDateTime cancelTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * TCC 状态枚举
     */
    public static class Status {
        public static final String TRYING = "TRYING";
        public static final String CONFIRMED = "CONFIRMED";
        public static final String CANCELLED = "CANCELLED";
    }
}