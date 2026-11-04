package com.cognora.platform.inventory.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cognora.platform.inventory.domain.ReservationStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * REST 库存预占记录（持久化状态机）。
 *
 * <p>幂等键为 (tenant_id, business_key)，同一租户同一业务键只允许一条记录。
 * 状态流转见 {@link ReservationStatus}。
 *
 * @author SCM Platform Team
 * @since 2026-11-04
 */
@Data
@TableName("inv_reservation_record")
public class InvReservationRecord {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    /** 业务键（订单号等），租户内唯一 */
    private String businessKey;

    private String skuId;

    private String warehouseId;

    /** 对应 inv_inventory.id，库存条件更新只针对这一行 */
    private String inventoryId;

    private Integer quantity;

    /** RESERVED / CONFIRMED / RELEASED / EXPIRED */
    private String status;

    private LocalDateTime expireAt;

    private LocalDateTime reservedAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime releasedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public ReservationStatus statusEnum() {
        return status == null ? null : ReservationStatus.valueOf(status);
    }
}
