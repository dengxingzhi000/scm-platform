package com.frog.inventory.domain.entity;

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

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务键（订单号等），用于幂等性
     */
    private String businessKey;

    /**
     * SKU ID
     */
    private Long skuId;

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
     * 状态: TRYING, CONFIRMED, CANCELLED
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