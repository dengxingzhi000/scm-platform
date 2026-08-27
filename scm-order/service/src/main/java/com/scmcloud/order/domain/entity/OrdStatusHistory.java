package com.scmcloud.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.scmcloud.common.domain.TenantId;
import java.time.LocalDateTime;
import java.util.UUID;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 璁㈠崟鐘舵佹祦杞巻锟?
 * </p>
 *
 * <p><b>职责</b>：订单状态变更的<b>操作者审计日志</b>。记录"谁在什么时间把订单从 X 状态改到 Y 状态"，
 * 以及变更原因 / 业务备注。供管理后台、客服系统、审计报表查询订单生命周期使用。</p>
 *
 * <p><b>与 {@link OrdOrderEvent} 的关系</b>：两者并存，记录不同维度，<b>不冗余</b>：</p>
 * <ul>
 *   <li>本表：操作者审计（{@code operatorId} / {@code operatorName} /
 *       {@code operatorType} / {@code remark} / {@code extraData}）</li>
 *   <li>{@link OrdOrderEvent}：事件 payload（{@code eventData} JSON 全字段）+ {@code eventId} 幂等键</li>
 * </ul>
 *
 * <p><b>写入路径</b>：由 {@code OrdStatusHistoryCommandService} 在 {@code OrdOrderCommandService}
 * 同一事务内写入，保证与状态变更原子提交。本表为状态变更审计日志的唯一来源。</p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ord_status_history")
public class OrdStatusHistory {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("order_id")
    private UUID orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("from_status")
    private Integer fromStatus;

    @TableField("to_status")
    private Integer toStatus;
    @TableField("event")
    private String event;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("operator_type")
    private String operatorType;

    @TableField("remark")
    private String remark;

    @TableField("extra_data")
    private String extraData;

    @TableField("transitioned_at")
    private LocalDateTime transitionedAt;
}
